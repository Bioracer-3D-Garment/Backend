package Bioracer.BachelorProject.Backend.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoleTest {

    @Test
    public void GivenRole_WhenConvertingToGrantedAuthority_ThenAuthorityHasRolePrefix(){
        assertEquals("ROLE_ADMIN", Role.ADMIN.toGrantedAuthority().getAuthority());
        assertEquals("ROLE_USER", Role.USER.toGrantedAuthority().getAuthority());
    }

    @Test
    public void GivenRole_WhenConvertingToString_ThenStringIsLowercase(){
        assertEquals("admin", Role.ADMIN.toString());
        assertEquals("user", Role.USER.toString());
    }

}
