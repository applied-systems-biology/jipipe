/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public class HashUtils {
    /**
     * Returns a float in [0,1] derived from a canonical SHA-256 of the map.
     * Small changes to keys/values will produce very different floats.
     */
    public static float toUnitFloat(Map<String, String> map) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Canonicalize: sort by key for deterministic ordering
            for (var e : new TreeMap<>(map).entrySet()) {
                putString(md, e.getKey());
                putString(md, e.getValue());
            }

            byte[] h = md.digest();

            // Use the first 24 bits for a uniform float bucket in [0,1]
            int n = ((h[0] & 0xFF) << 16) | ((h[1] & 0xFF) << 8) | (h[2] & 0xFF);
            return (float)(n / 16777215.0); // 2^24 - 1  -> allows 1.0f occasionally
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void putString(MessageDigest md, String s) {
        if (s == null) {
            md.update(int4(-1)); // null marker
            return;
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        md.update(int4(bytes.length)); // unambiguous length prefix
        md.update(bytes);
    }

    private static byte[] int4(int v) {
        return ByteBuffer.allocate(4).putInt(v).array(); // big-endian
    }
}
