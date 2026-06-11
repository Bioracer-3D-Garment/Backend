package Bioracer.BachelorProject.Backend.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GeneratedAssetTest {

    private final User user = new User("Thomas", "Bosmans", "thomas@example.com", "validpassword", Role.USER);
    private final Project project = new Project("Summer Collection", user);

    @Test
    public void GivenValidGeneratedAsset_WhenCreatingGeneratedAsset_ThenGeneratedAssetIsCreated(){
        GeneratedAsset asset = new GeneratedAsset(project, "validsecureurl", "validthumbnailurl", "validpublicid");

        assertEquals(project, asset.getProject());
        assertEquals("validsecureurl", asset.getSecureUrl());
        assertEquals("validthumbnailurl", asset.getThumbnailUrl());
        assertEquals("validpublicid", asset.getPublicId());
        assertNull(asset.getJobId());
        assertNull(asset.getProductId());
        assertNull(asset.getPoseId());
        assertNull(asset.getCategory());
        assertNotNull(asset.getCreatedAt());
    }

    @Test
    public void GivenValidGeneratedAsset_WhenCreatingGeneratedAssetWithJobDetails_ThenGeneratedAssetIsCreated(){
        GeneratedAsset asset = new GeneratedAsset(project, "validjobid", "validproductid", "validposeid",
                "validcategory", "validsecureurl", "validthumbnailurl", "validpublicid");

        assertEquals(project, asset.getProject());
        assertEquals("validjobid", asset.getJobId());
        assertEquals("validproductid", asset.getProductId());
        assertEquals("validposeid", asset.getPoseId());
        assertEquals("validcategory", asset.getCategory());
        assertEquals("validsecureurl", asset.getSecureUrl());
        assertEquals("validthumbnailurl", asset.getThumbnailUrl());
        assertEquals("validpublicid", asset.getPublicId());
        assertNotNull(asset.getCreatedAt());
    }

}
