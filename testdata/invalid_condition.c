// A file with an invalid / unparseable condition
#if 2 > MAX(-1, 1)
    someCode();
#endif
