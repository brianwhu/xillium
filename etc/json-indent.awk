function p(indent, text, subscript, array) {
    if (indent > 0) {
        printf "%"indent"s", "";
    }
    if (subscript[array] > 0) {
        printf "[%04d] %s\n", (subscript[array]-1), text;
        ++subscript[array];
    } else {
        print text;
    }
}
BEGIN { indent = -2; array = 0; }
/^{/  { indent += 2; p(indent, "{", subscript, array); ++array; subscript[array] = 0; }
/^\[/ { indent += 2; p(indent, "{", subscript, array); ++array; subscript[array] = 1; }
/^]/  { p(indent, "}"); indent -= 2; --array; }
/^}/  { p(indent, "}"); indent -= 2; --array; }
/^[^{}[\]]/ { p(indent+2, $0, subscript, array); }
