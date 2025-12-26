package com.demo.adventure.authoring.gardener;

import com.demo.adventure.domain.save.GameSave;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Computes a deterministic fingerprint for a world to pair with the Gardener patch.
 */
public final class WorldFingerprint {
    private WorldFingerprint() {
    }

    // Pattern: Learning + Verification
    // - Generates a stable world fingerprint to tie patches back to the exact source snapshot.
    public static String fingerprint(GameSave save) {
        if (save == null) {
            return "";
        }
        List<String> ids = new ArrayList<>();
        save.plots().forEach(p -> ids.add("P:" + safe(p.plotId())));
        save.gates().forEach(g -> ids.add("G:" + safe(g.fromPlotId()) + "->" + safe(g.toPlotId())));
        save.fixtures().forEach(f -> ids.add("F:" + safe(f.id())));
        save.items().forEach(i -> ids.add("I:" + safe(i.id())));
        save.actors().forEach(a -> ids.add("A:" + safe(a.id())));
        Collections.sort(ids);
        String joined = String.join("|", ids);
        return sha256(joined);
    }

    private static String safe(UUID id) {
        return id == null ? "null" : id.toString();
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
