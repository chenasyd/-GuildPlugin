package com.guild.module.example.territory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerritoryServerIdentityTest {

    @Test
    void generatesAndPersistsRandomId(@TempDir File dir) throws Exception {
        Logger logger = Logger.getLogger("test");

        TerritoryServerIdentity first = TerritoryServerIdentity.resolve(dir, null, logger);
        assertTrue(first.getServerId().startsWith("terr-"));
        assertEquals(17, first.getServerId().length());

        TerritoryServerIdentity second = TerritoryServerIdentity.resolve(dir, null, logger);
        assertEquals(first.getServerId(), second.getServerId());

        File idFile = new File(dir, TerritoryServerIdentity.ID_FILE);
        assertTrue(idFile.isFile());
        assertEquals(first.getServerId(), Files.readString(idFile.toPath()).trim());
    }

    @Test
    void forTestsProvidesStableId() {
        assertEquals("manual-server-1", TerritoryServerIdentity.forTests("manual-server-1").getServerId());
    }
}
