//! Simple heap using reference-counted objects.
//!
//! Each [`JObject`] is represented as a reference-counted smart pointer.
//! This avoids a full GC implementation while being sufficient for short-lived
//! decoder invocations like Raoh.

use std::cell::RefCell;
use std::collections::HashMap;
use std::hash::{Hash, Hasher};
use std::rc::Rc;

/// Native backing storage for `java.lang.String`.
///
/// The canonical payload is UTF-16 code units so VM-visible string indices can
/// follow Java semantics even when the content includes surrogate pairs or lone
/// surrogates created by slicing.
#[derive(Clone)]
pub struct JavaStringValue {
    utf16: Vec<u16>,
    utf8: Option<String>,
}

impl JavaStringValue {
    pub fn new(s: impl Into<String>) -> Self {
        let utf8 = s.into();
        Self {
            utf16: utf8.encode_utf16().collect(),
            utf8: Some(utf8),
        }
    }

    pub fn from_utf16(utf16: Vec<u16>) -> Self {
        let utf8 = String::from_utf16(&utf16).ok();
        Self { utf16, utf8 }
    }

    pub fn as_str(&self) -> Option<&str> {
        self.utf8.as_deref()
    }

    pub fn to_string_lossy(&self) -> String {
        self.utf8
            .clone()
            .unwrap_or_else(|| String::from_utf16_lossy(&self.utf16))
    }

    pub fn utf16(&self) -> &[u16] {
        &self.utf16
    }

    pub fn len_utf16(&self) -> usize {
        self.utf16.len()
    }

    pub fn code_unit_at(&self, index: usize) -> Option<u16> {
        self.utf16.get(index).copied()
    }

    pub fn slice_utf16(&self, start: usize, end: usize) -> Vec<u16> {
        let len = self.utf16.len();
        let start = start.min(len);
        let end = end.min(len).max(start);
        self.utf16[start..end].to_vec()
    }

    pub fn concat(&self, other: &Self) -> Self {
        let mut utf16 = Vec::with_capacity(self.utf16.len() + other.utf16.len());
        utf16.extend_from_slice(&self.utf16);
        utf16.extend_from_slice(&other.utf16);
        let utf8 = match (&self.utf8, &other.utf8) {
            (Some(left), Some(right)) => {
                let mut s = String::with_capacity(left.len() + right.len());
                s.push_str(left);
                s.push_str(right);
                Some(s)
            }
            _ => None,
        };
        Self { utf16, utf8 }
    }

    pub fn hash_code(&self) -> i32 {
        self.utf16.iter().fold(0i32, |hash, code_unit| {
            hash.wrapping_mul(31).wrapping_add(i32::from(*code_unit))
        })
    }
}

impl PartialEq for JavaStringValue {
    fn eq(&self, other: &Self) -> bool {
        self.utf16 == other.utf16
    }
}

impl Eq for JavaStringValue {}

impl Hash for JavaStringValue {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.utf16.hash(state);
    }
}

impl std::fmt::Debug for JavaStringValue {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "JavaStringValue({:?}, utf16_len={})",
            self.to_string_lossy(),
            self.utf16.len()
        )
    }
}

/// A Java value that can appear on the operand stack or in a local variable slot.
#[derive(Debug, Clone)]
pub enum JValue {
    Void,
    Int(i32),
    Long(i64),
    Float(f32),
    Double(f64),
    /// Reference to a heap-allocated object (or null).
    Ref(Option<JRef>),
    /// Return address (used by `jsr`/`ret`, rare in modern bytecode).
    ReturnAddress(u32),
}

impl JValue {
    /// Unwrap as `i32` or panic.
    pub fn as_int(&self) -> i32 {
        match self {
            JValue::Int(v) => *v,
            other => panic!("Expected Int, got {other:?}"),
        }
    }

    /// Unwrap as `i64` or panic.
    pub fn as_long(&self) -> i64 {
        match self {
            JValue::Long(v) => *v,
            other => panic!("Expected Long, got {other:?}"),
        }
    }

    /// Unwrap as `f32` or panic.
    pub fn as_float(&self) -> f32 {
        match self {
            JValue::Float(v) => *v,
            other => panic!("Expected Float, got {other:?}"),
        }
    }

    /// Unwrap as `f64` or panic.
    pub fn as_double(&self) -> f64 {
        match self {
            JValue::Double(v) => *v,
            other => panic!("Expected Double, got {other:?}"),
        }
    }

    /// Unwrap as object reference (may be null).
    /// `Int(0)` is treated as null because uninitialized local slots and
    /// some bytecode paths (e.g. `iconst_0` used as `aconst_null` equivalent)
    /// may leave an Int(0) where a reference is expected.
    pub fn as_ref(&self) -> Option<&JRef> {
        match self {
            JValue::Ref(r) => r.as_ref(),
            JValue::Void | JValue::Int(0) => None,
            other => panic!("Expected Ref, got {other:?}"),
        }
    }

    /// Returns `true` if this is a null reference.
    pub fn is_null(&self) -> bool {
        matches!(self, JValue::Ref(None))
    }
}

/// A reference-counted handle to a heap object.
pub type JRef = Rc<RefCell<JObject>>;

/// A heap-allocated Java object.
#[derive(Debug)]
pub struct JObject {
    /// The fully-qualified internal class name (e.g. `"java/lang/String"`).
    pub class_name: String,
    /// Instance fields keyed by field name.
    pub fields: HashMap<String, JValue>,
    /// Underlying native payload for special types (String content, arrays, etc.).
    pub native: NativePayload,
}

/// Native backing storage for built-in types.
pub enum NativePayload {
    None,
    /// `java.lang.String` content.
    JavaString(JavaStringValue),
    /// Object array (`[Ljava/lang/Object;` etc.).
    Array(Vec<JValue>),
    /// Byte/char/int primitive arrays.
    ByteArray(Vec<u8>),
    IntArray(Vec<i32>),
    LongArray(Vec<i64>),
    /// `java.io.PrintStream` marker (`false` => stdout, `true` => stderr).
    PrintStream(bool),
    /// `java.io.ProcessPipeInputStream` marker for launcher stdin.
    ProcessPipeInputStream,
    /// A Rust closure captured as a lambda stand-in.
    Lambda(Rc<dyn Fn(Vec<JValue>) -> JValue>),
    /// A lambda backed by a bytecode method handle.
    ///
    /// When the functional interface method is invoked, the VM looks up
    /// `impl_class::impl_method(impl_desc)` and prepends `captured` to the arguments.
    BytecodeLambda {
        /// Functional interface method name (SAM), e.g. "apply", "decode".
        sam_method: String,
        /// Functional interface method descriptor for the SAM.
        sam_desc: String,
        impl_class: String,
        impl_method: String,
        impl_desc: String,
        /// JVM reference_kind: 5=invokeVirtual, 6=invokeStatic, 7=invokeSpecial, etc.
        ref_kind: u8,
        captured: Vec<JValue>,
    },
    /// A record method handle created by `java/lang/runtime/ObjectMethods` bootstrap.
    ///
    /// Implements `toString`, `equals`, or `hashCode` for a record class by
    /// calling each component's getter (invokeVirtual ref_kind=5).
    RecordMethod {
        /// The method being implemented: "toString", "equals", or "hashCode".
        method: String,
        /// Simple class name used in toString output (e.g. "Present").
        class_simple_name: String,
        /// Component names in declaration order (used for toString).
        component_names: Vec<String>,
        /// Getter method handles: (class_name, method_name, descriptor).
        getters: Vec<(String, String, String)>,
    },
}

impl std::fmt::Debug for NativePayload {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            NativePayload::None => write!(f, "None"),
            NativePayload::JavaString(s) => write!(f, "JavaString({s:?})"),
            NativePayload::Array(v) => write!(f, "Array(len={})", v.len()),
            NativePayload::ByteArray(v) => write!(f, "ByteArray(len={})", v.len()),
            NativePayload::IntArray(v) => write!(f, "IntArray(len={})", v.len()),
            NativePayload::LongArray(v) => write!(f, "LongArray(len={})", v.len()),
            NativePayload::PrintStream(is_err) => write!(f, "PrintStream(err={is_err})"),
            NativePayload::ProcessPipeInputStream => write!(f, "ProcessPipeInputStream"),
            NativePayload::Lambda(_) => write!(f, "Lambda(...)"),
            NativePayload::BytecodeLambda {
                impl_class,
                impl_method,
                ..
            } => {
                write!(f, "BytecodeLambda({impl_class}::{impl_method})")
            }
            NativePayload::RecordMethod {
                method,
                class_simple_name,
                ..
            } => {
                write!(f, "RecordMethod({class_simple_name}::{method})")
            }
        }
    }
}

impl JObject {
    /// Create a plain Java object with the given class name.
    pub fn new(class_name: impl Into<String>) -> JRef {
        Rc::new(RefCell::new(JObject {
            class_name: class_name.into(),
            fields: HashMap::new(),
            native: NativePayload::None,
        }))
    }

    /// Create a `java.lang.String` backed by a Rust `String`.
    pub fn new_string(s: impl Into<String>) -> JRef {
        Rc::new(RefCell::new(JObject {
            class_name: "java/lang/String".to_owned(),
            fields: HashMap::new(),
            native: NativePayload::JavaString(JavaStringValue::new(s)),
        }))
    }

    /// Create a `java.lang.String` backed by raw UTF-16 code units.
    pub fn new_string_utf16(utf16: Vec<u16>) -> JRef {
        Self::new_string_value(JavaStringValue::from_utf16(utf16))
    }

    /// Create a `java.lang.String` backed by an existing UTF-16 value.
    pub fn new_string_value(value: JavaStringValue) -> JRef {
        Rc::new(RefCell::new(JObject {
            class_name: "java/lang/String".to_owned(),
            fields: HashMap::new(),
            native: NativePayload::JavaString(value),
        }))
    }

    /// Create an object array.
    pub fn new_array(class_name: impl Into<String>, elements: Vec<JValue>) -> JRef {
        Rc::new(RefCell::new(JObject {
            class_name: class_name.into(),
            fields: HashMap::new(),
            native: NativePayload::Array(elements),
        }))
    }

    /// Create a lambda/closure object.
    pub fn new_lambda(f: impl Fn(Vec<JValue>) -> JValue + 'static) -> JRef {
        Rc::new(RefCell::new(JObject {
            class_name: "$$Lambda".to_owned(),
            fields: HashMap::new(),
            native: NativePayload::Lambda(Rc::new(f)),
        }))
    }

    /// Create a `java.io.PrintStream` marker object.
    pub fn new_print_stream(is_err: bool) -> JRef {
        Rc::new(RefCell::new(JObject {
            class_name: "java/io/PrintStream".to_owned(),
            fields: HashMap::new(),
            native: NativePayload::PrintStream(is_err),
        }))
    }

    /// Create a `java.io.ProcessPipeInputStream` marker object.
    pub fn new_process_pipe_input_stream() -> JRef {
        Rc::new(RefCell::new(JObject {
            class_name: "java/io/ProcessPipeInputStream".to_owned(),
            fields: HashMap::new(),
            native: NativePayload::ProcessPipeInputStream,
        }))
    }

    /// Get the string content if this is a `java.lang.String`.
    pub fn as_java_string(&self) -> Option<&str> {
        match &self.native {
            NativePayload::JavaString(s) => s.as_str(),
            _ => None,
        }
    }

    pub fn as_java_string_value(&self) -> Option<&JavaStringValue> {
        match &self.native {
            NativePayload::JavaString(s) => Some(s),
            _ => None,
        }
    }

    pub fn as_java_string_utf16(&self) -> Option<&[u16]> {
        self.as_java_string_value().map(JavaStringValue::utf16)
    }

    pub fn java_string_to_string_lossy(&self) -> Option<String> {
        self.as_java_string_value().map(JavaStringValue::to_string_lossy)
    }
}
