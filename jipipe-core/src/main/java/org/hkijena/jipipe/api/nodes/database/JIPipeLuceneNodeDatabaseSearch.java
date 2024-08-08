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

package org.hkijena.jipipe.api.nodes.database;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

public class JIPipeLuceneNodeDatabaseSearch {

    private final JIPipeNodeDatabase nodeDatabase;
    private final Directory directory = new ByteBuffersDirectory();

    public JIPipeLuceneNodeDatabaseSearch(JIPipeNodeDatabase nodeDatabase) {
        this.nodeDatabase = nodeDatabase;
    }

    public List<JIPipeNodeDatabaseEntry> query(String queryString, JIPipeNodeDatabaseRole role, boolean allowExisting, boolean allowNew, int numResults) {
        if (StringUtils.isNullOrEmpty(queryString) || StringUtils.isNullOrEmpty(queryString.trim())) {
            List<JIPipeNodeDatabaseEntry> entryResults = new ArrayList<>();
            for (JIPipeNodeDatabaseEntry entry : nodeDatabase.getEntries()) {
                if (entry.getRole() != role)
                    continue;
                if (entry.exists() && !allowExisting)
                    continue;
                if (!entry.exists() && !allowNew)
                    continue;
                entryResults.add(entry);
            }
            return entryResults;
        } else {
            try {
                Analyzer analyzer = new FuzzySearchAnalyzer();

                Map<String, Float> boosts = new HashMap<>();
                boosts.put("name", 8f);
                boosts.put("location", 3f);
//                boosts.put("description", 1.0f);

                MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[]{"name", "location"}, analyzer, boosts);
                Query query = queryParser.parse(queryString);

                List<JIPipeNodeDatabaseEntry> entryResults = new ArrayList<>();

                try (DirectoryReader reader = DirectoryReader.open(directory)) {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    TopDocs results = searcher.search(query, numResults);
                    ScoreDoc[] hits = results.scoreDocs;

                    System.out.println("'" + queryString + "' ---> " + hits.length);
                    for (ScoreDoc hit : hits) {
                        Document doc = searcher.doc(hit.doc);
                        int index = Integer.parseInt(doc.get("id"));
                        System.out.println(hit.score + " --> " +  "Found item ID: " + index + ", Name: " + doc.get("name") + " # " + renderTokens(analyzer, doc.get("name")));
                        JIPipeNodeDatabaseEntry entry = nodeDatabase.getEntries().get(index);

                        if (entry.getRole() != role)
                            continue;
                        if (entry.exists() && !allowExisting)
                            continue;
                        if (!entry.exists() && !allowNew)
                            continue;

                        entryResults.add(entry);
                    }
                }

                return entryResults;
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
    }

    public static String renderTokens(Analyzer analyzer, String text) throws Exception {
        TokenStream tokenStream = analyzer.tokenStream(null, text);
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        StringBuilder builder = new StringBuilder();
        tokenStream.reset();
        boolean first = true;
        while (tokenStream.incrementToken()) {
            String token = charTermAttribute.toString();
            builder.append(token);
            if(!first) {
                builder.append(" ");
            }
            else {
                first = false;
            }
        }
        tokenStream.end();
        tokenStream.close();

        return builder.toString();
    }

    // Method to clear the Directory
    public void clearLuceneDirectory() throws Exception {
        IndexWriterConfig config = new IndexWriterConfig();
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            // Delete all documents in the directory
            writer.deleteAll();
            writer.commit(); // Ensure the deletions are committed
        }
    }


    public void rebuildDirectory() throws Exception {
        clearLuceneDirectory();

        long currentTimeMillis = System.currentTimeMillis();

        IndexWriterConfig config = new IndexWriterConfig();
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            for (int i = 0; i < nodeDatabase.getEntries().size(); i++) {
                JIPipeNodeDatabaseEntry entry = nodeDatabase.getEntries().get(i);
                Document doc = new Document();
                doc.add(new StringField("id", String.valueOf(i), Field.Store.YES));
                doc.add(new TextField("name", entry.getName(), Field.Store.YES));
                doc.add(new TextField("description", entry.getDescriptionPlain(), Field.Store.YES));
                doc.add(new TextField("location", entry.getLocationInfo(), Field.Store.YES));
                writer.addDocument(doc);
            }
            writer.commit();
        }

        System.out.println("Rebuild took " + (System.currentTimeMillis() - currentTimeMillis) + " ms");

    }


    public static class FuzzySearchAnalyzer extends Analyzer {

        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
//            Tokenizer src = new EdgeNGramTokenizer(1, 20);
            Tokenizer src = new StandardTokenizer();
            TokenStream result = new LowerCaseFilter(src);
            result = new EdgeNGramTokenFilter(result, 1, 20, true);
            result = new PorterStemFilter(result);
            return new TokenStreamComponents(src, result);
        }
    }
}
