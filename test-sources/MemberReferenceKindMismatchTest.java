public class MemberReferenceKindMismatchTest {
    public static String run() throws Exception {
        String methodrefToInterface = LoaderPhase0Fixtures.repeatedFailure(
                "phase0.MethodrefToInterfaceCaller",
                new String[] { "phase0.MethodrefToInterfaceCaller", "phase0.InterfaceTarget" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytesWithMethodReferenceTag(
                            LoaderPhase0Fixtures.METHODREF_TO_INTERFACE_CALLER_B64,
                            "phase0/InterfaceTarget",
                            "value",
                            10),
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.INTERFACE_TARGET_B64)
                });
        String interfaceMethodrefToClass = LoaderPhase0Fixtures.repeatedFailure(
                "phase0.InterfaceMethodrefToClassCaller",
                new String[] { "phase0.InterfaceMethodrefToClassCaller", "phase0.ClassTarget" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytesWithMethodReferenceTag(
                            LoaderPhase0Fixtures.INTERFACE_METHODREF_TO_CLASS_CALLER_B64,
                            "phase0/ClassTarget",
                            "value",
                            11),
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.CLASS_TARGET_B64)
                });
        return new StringBuilder()
                .append(methodrefToInterface)
                .append('|')
                .append(interfaceMethodrefToClass)
                .toString();
    }
}
