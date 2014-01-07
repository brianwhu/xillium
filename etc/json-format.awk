# assume that the input is a single line of JSON string
BEGIN { step = 0; }
{
    # step 1: hide away all quoted strings into array "data"
    text = $0;
    do {
        match(text, /("[^"\\]*(\\.[^"\\]*)*")/, a);
        if (1 in a) {
            data[step] = substr(text, a[1,"start"], a[1,"length"]);
            text = substr(text, 1, a[1,"start"] - 1) "<" step ">" substr(text, a[1,"start"] + a[1,"length"]);
            ++step;
        }
    } while (1 in a);
    # step 2: format transformed text "text"
    gsub(/ *{ */, "\n{\n", text);
    gsub(/ *} */, "\n}", text);
    gsub(/ *, */, "\n", text);
    gsub(/ *\[ */, "\n[\n", text);
    gsub(/ *] */, "\n]", text);
    gsub(/^\n/, "", text);
    gsub(/\n\n*/, "\n", text);
}
END {
    for (step in data) {
        gsub("<" step ">", data[step], text);
    }
    gsub(/\\n/, "\n", text);
    gsub(/\\t/, "\t", text);
    print text;
}
