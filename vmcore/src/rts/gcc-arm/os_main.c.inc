/* entry point */

int main(int argc, char *argv[]) {
    diagnostic("In main - squawk.c");
    return Squawk_main_wrapper(argc, argv);
}
