package kz.agromarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kz.agromarket.auth.RegisterRequest;
import kz.agromarket.auth.Role;
import kz.agromarket.auth.User;
import kz.agromarket.auth.VerificationStatus;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.UploadedFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US-01, US-02. Тіркелу және верификация")
class AuthTest {

    private final AgroMarketApp app = new AgroMarketApp();

    private User register(String email, Role role) {
        return app.auth.register(new RegisterRequest(email, "secret123", "Айдар", role));
    }

    @Test
    @DisplayName("US-01: фермер тіркеледі, мәртебесі PENDING")
    void farmerRegistersAsPending() {
        User farmer = register("f@test.kz", Role.FARMER);
        assertEquals(Role.FARMER, farmer.getRole());
        assertEquals(VerificationStatus.PENDING, farmer.getVerificationStatus());
    }

    @Test
    @DisplayName("US-01: құпиясөз ашық түрде сақталмайды, бірақ кіру жұмыс істейді")
    void passwordIsHashed() {
        User user = register("h@test.kz", Role.BUYER);
        assertFalse(user.getPasswordHash().contains("secret123"));
        assertEquals(user.getId(), app.auth.login("h@test.kz", "secret123").getId());
        assertThrows(DomainException.class, () -> app.auth.login("h@test.kz", "wrong-pass"));
    }

    @Test
    @DisplayName("US-01: бірдей email-мен қайта тіркелу — 409 қате")
    void duplicateEmailRejected() {
        register("d@test.kz", Role.FARMER);
        DomainException e = assertThrows(DomainException.class, () -> register("d@test.kz", Role.FARMER));
        assertEquals(409, e.getStatus());
    }

    @Test
    @DisplayName("US-01: құжат PDF/JPG форматында және 5 МБ-тан аспауы керек")
    void documentValidation() {
        String id = register("doc@test.kz", Role.FARMER).getId();
        assertThrows(DomainException.class, () -> app.auth.uploadDocument(id, new UploadedFile("doc.exe", 100)));
        assertThrows(DomainException.class, () -> app.auth.uploadDocument(id, new UploadedFile("doc.pdf", 6L * 1024 * 1024)));
        assertEquals("minio://documents/" + id + "/doc.pdf",
                app.auth.uploadDocument(id, new UploadedFile("doc.pdf", 1000)).getDocumentUrl());
    }

    @Test
    @DisplayName("US-02: модератор фермерді растайды, фермер хабарлама алады")
    void moderatorApprovesFarmer() {
        String farmer = register("v@test.kz", Role.FARMER).getId();
        String mod = register("m@test.kz", Role.MODERATOR).getId();

        assertEquals(1, app.auth.listPendingFarmers(mod).size());
        app.auth.approveFarmer(mod, farmer);

        assertTrue(app.auth.isVerifiedFarmer(farmer));
        assertEquals(1, app.notifications.forUser(farmer).size());
    }

    @Test
    @DisplayName("US-02: бас тарту кезінде себеп міндетті")
    void rejectRequiresReason() {
        TestSupport t = new TestSupport();
        String farmer = t.register("r", Role.FARMER);
        assertThrows(DomainException.class, () -> t.app.auth.rejectFarmer(t.moderator, farmer, " "));
        t.app.auth.rejectFarmer(t.moderator, farmer, "Құжат анық емес");
        assertFalse(t.app.auth.isVerifiedFarmer(farmer));
    }

    @Test
    @DisplayName("US-02: басқа рөл верификация жасай алмайды — 403")
    void onlyModeratorCanVerify() {
        TestSupport t = new TestSupport();
        DomainException e = assertThrows(DomainException.class, () -> t.app.auth.approveFarmer(t.buyer, t.farmerA));
        assertEquals(403, e.getStatus());
    }
}
