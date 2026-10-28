package na.eat.marketplace.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Role-aware access (SRS 7.4) and browsing without an account (FR-AC-06). */
@SpringBootTest
@AutoConfigureMockMvc
class AccessTest {

    @Autowired
    MockMvc mvc;

    @Test
    void guestsCanBrowse() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/search").param("q", "stew")).andExpect(status().isOk());
        mvc.perform(get("/cart")).andExpect(status().isOk());
    }

    @Test
    void checkoutAndManagementNeedLogin() throws Exception {
        mvc.perform(get("/checkout")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/seller")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customersCannotOpenSellerOrAdminPages() throws Exception {
        mvc.perform(get("/seller")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/sellers")).andExpect(status().isForbidden());
    }

    @Test
    void formsNeedCsrfToken() throws Exception {
        mvc.perform(post("/cart/clear")).andExpect(status().isForbidden());
    }
}
