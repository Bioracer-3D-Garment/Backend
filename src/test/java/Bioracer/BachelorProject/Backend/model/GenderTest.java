package Bioracer.BachelorProject.Backend.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenderTest {

    @Test
    public void GivenGenderEnum_WhenListingValues_ThenOrderIsStable(){
        // genders are persisted by ordinal, so the declaration order must never change
        assertArrayEquals(new Gender[]{Gender.MALE, Gender.FEMALE, Gender.X}, Gender.values());
    }

    @Test
    public void GivenGenderName_WhenParsing_ThenGenderIsResolved(){
        assertEquals(Gender.MALE, Gender.valueOf("MALE"));
        assertEquals(Gender.FEMALE, Gender.valueOf("FEMALE"));
        assertEquals(Gender.X, Gender.valueOf("X"));
    }

}
