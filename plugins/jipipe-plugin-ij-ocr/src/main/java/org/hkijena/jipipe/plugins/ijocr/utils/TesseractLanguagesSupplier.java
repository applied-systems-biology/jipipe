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

package org.hkijena.jipipe.plugins.ijocr.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TesseractLanguagesSupplier implements Supplier<List<Object>> {

    public static final String[] LANGUAGES = new String[]{
            "afr",
            "amh",
            "ara",
            "asm",
            "aze",
            "aze_cyrl",
            "bel",
            "ben",
            "bod",
            "bos",
            "bre",
            "bul",
            "cat",
            "ceb",
            "ces",
            "chi_sim",
            "chi_sim_vert",
            "chi_tra",
            "chi_tra_vert",
            "chr",
            "cos",
            "cym",
            "dan",
            "deu",
            "deu_latf",
            "div",
            "dzo",
            "ell",
            "eng",
            "enm",
            "epo",
            "equ",
            "est",
            "eus",
            "fao",
            "fas",
            "fil",
            "fin",
            "fra",
            "frm",
            "fry",
            "gla",
            "gle",
            "glg",
            "grc",
            "guj",
            "hat",
            "heb",
            "hin",
            "hrv",
            "hun",
            "hye",
            "iku",
            "ind",
            "isl",
            "ita",
            "ita_old",
            "jav",
            "jpn",
            "jpn_vert",
            "kan",
            "kat",
            "kat_old",
            "kaz",
            "khm",
            "kir",
            "kmr",
            "kor",
            "lao",
            "lat",
            "lav",
            "lit",
            "ltz",
            "mal",
            "mar",
            "mkd",
            "mlt",
            "mon",
            "mri",
            "msa",
            "mya",
            "nep",
            "nld",
            "nor",
            "oci",
            "ori",
            "osd",
            "pan",
            "pol",
            "por",
            "pus",
            "que",
            "ron",
            "rus",
            "san",
            "script\\Arabic",
            "script\\Armenian",
            "script\\Bengali",
            "script\\Canadian_Aboriginal",
            "script\\Cherokee",
            "script\\Cyrillic",
            "script\\Devanagari",
            "script\\Ethiopic",
            "script\\Fraktur",
            "script\\Georgian",
            "script\\Greek",
            "script\\Gujarati",
            "script\\Gurmukhi",
            "script\\HanS",
            "script\\HanS_vert",
            "script\\HanT",
            "script\\HanT_vert",
            "script\\Hangul",
            "script\\Hangul_vert",
            "script\\Hebrew",
            "script\\Japanese",
            "script\\Japanese_vert",
            "script\\Kannada",
            "script\\Khmer",
            "script\\Lao",
            "script\\Latin",
            "script\\Malayalam",
            "script\\Myanmar",
            "script\\Oriya",
            "script\\Sinhala",
            "script\\Syriac",
            "script\\Tamil",
            "script\\Telugu",
            "script\\Thaana",
            "script\\Thai",
            "script\\Tibetan",
            "script\\Vietnamese",
            "sin",
            "slk",
            "slv",
            "snd",
            "spa",
            "spa_old",
            "sqi",
            "srp",
            "srp_latn",
            "sun",
            "swa",
            "swe",
            "syr",
            "tam",
            "tat",
            "tel",
            "tgk",
            "tha",
            "tir",
            "ton",
            "tur",
            "uig",
            "ukr",
            "urd",
            "uzb",
            "uzb_cyrl",
            "vie",
            "yid",
            "yor"
    };

    @Override
    public List<Object> get() {
        return Arrays.stream(LANGUAGES).collect(Collectors.toList());
    }
}
