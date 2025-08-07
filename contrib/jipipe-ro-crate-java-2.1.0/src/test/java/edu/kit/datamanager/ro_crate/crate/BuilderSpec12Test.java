package edu.kit.datamanager.ro_crate.crate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import edu.kit.datamanager.ro_crate.reader.Readers;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.special.CrateVersion;
import edu.kit.datamanager.ro_crate.validation.JsonSchemaValidation;
import edu.kit.datamanager.ro_crate.validation.Validator;

class BuilderSpec12Test {
    private URI profile1;
    private URI profile2;

    @Test
    void testAppendConformsTo() throws URISyntaxException {
        Crate crate = new RoCrate.BuilderWithDraftFeatures()
                .alsoConformsTo(new URI("https://w3id.org/ro/wfrun/process/0.1"))
                .alsoConformsTo(new URI("https://example.com/myprofile/1.0"))
                .build();
        JsonNode conformsTo = crate.getJsonDescriptor().getProperty("conformsTo");
        assertTrue(conformsTo.isArray());
        // one version and two profiles
        assertEquals(1 + 2, conformsTo.size());
    }

    @Test
    void testModificationOfDraftCrate() throws URISyntaxException, IOException {
        String path = this.getClass().getResource("/crates/spec-1.2-DRAFT/minimal-with-conformsTo-Array").getPath();
        RoCrate crate = Readers.newFolderReader().readCrate(path);
        Collection<String> existingProfiles = crate.getProfiles();
        profile1 = new URI("https://example.com/myprofile/1.0");
        profile2 = new URI("https://example.com/myprofile/2.0");
        // the loaded crate has at least one profile
        assertFalse(existingProfiles.isEmpty());
        // and the ones we will add later are not part of it
        assertFalse(existingProfiles.contains(profile1.toString()));
        assertFalse(existingProfiles.contains(profile2.toString()));

        // add profiles
        RoCrate modifiedCrate = new RoCrate.BuilderWithDraftFeatures(crate)
                .alsoConformsTo(profile1)
                .alsoConformsTo(profile2)
                .addContextualEntity(new ContextualEntity.ContextualEntityBuilder()
                        .addType("CreativeWork")
                        .build())
                .build();
        
        // sanity checks
        Validator defaultValidation = new Validator(new JsonSchemaValidation());
        assertTrue(defaultValidation.validate(modifiedCrate));
        assertEquals(CrateVersion.LATEST_UNSTABLE, crate.getVersion().get());
        assertEquals(CrateVersion.LATEST_UNSTABLE, modifiedCrate.getVersion().get());
        
        // number of profiles increased by 2
        Collection<String> newProfileState = modifiedCrate.getProfiles();
        assertEquals(existingProfiles.size() + 2, newProfileState.size());
        // new profiles are present
        assertTrue(newProfileState.contains(profile1.toString()));
        assertTrue(newProfileState.contains(profile2.toString()));
        // old profiles are present
        assertEquals(
            0,
            existingProfiles.stream()
                .filter(txt -> !newProfileState.contains(txt))
                .count()
        );
    }
}
