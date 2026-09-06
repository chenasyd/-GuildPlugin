package com.guild.core.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageResolverTest {

    @Test
    void resolve_returnsDefaultWhenConfigNull() {
        assertEquals("fallback", MessageResolver.resolve(null, "any.path", "fallback"));
    }

    @Test
    void withPlaceholders_replacesPairs() {
        String result = MessageResolver.withPlaceholders("Hello {name}, balance {bal}",
                "{name}", "Alice", "{bal}", "100");
        assertEquals("Hello Alice, balance 100", result);
    }

    @Test
    void withPlaceholders_nullValueBecomesEmpty() {
        assertEquals("x", MessageResolver.withPlaceholders("x{y}", "{y}", null));
    }

    @Test
    void withIndexedArgs_replacesNumericPlaceholders() {
        assertEquals("a=1 b=2",
                MessageResolver.withIndexedArgs("a={0} b={1}", "1", "2"));
    }

    @Test
    void withIndexedArgs_nullArgBecomesEmpty() {
        assertEquals("v=", MessageResolver.withIndexedArgs("v={0}", (String[]) new String[]{null}));
    }

    @Test
    void colorize_convertsAmpersandCodes() {
        assertEquals("\u00a7aGreen", MessageResolver.colorize("&aGreen"));
    }

    @Test
    void resolveWithPlaceholders_composesLookupAndReplace() {
        var config = LanguageConfigMerge.loadYaml("greeting: Hello {who}");
        assertEquals("Hello Bob",
                MessageResolver.resolveWithPlaceholders(config, "greeting", "Hi", "{who}", "Bob"));
    }

    @Test
    void resolveWithIndexedArgs_composesLookupAndReplace() {
        var config = LanguageConfigMerge.loadYaml("line: Item {0} x {1}");
        assertEquals("Item sword x 3",
                MessageResolver.resolveWithIndexedArgs(config, "line", "", "sword", "3"));
    }
}
