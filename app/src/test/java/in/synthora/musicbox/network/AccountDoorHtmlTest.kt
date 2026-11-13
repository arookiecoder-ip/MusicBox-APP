package `in`.synthora.musicbox.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDoorHtmlTest {
    @Test
    fun readsCsrfFromTheRenderedForm() {
        val html = """<form><input type="hidden" name="_csrf" value="token-123"></form>"""
        assertEquals("token-123", DoorHtml.csrf(html))
    }

    @Test
    fun mapsEachFieldErrorToItsInput() {
        val html = """
            <label for="username">Username</label>
            <input id="username" name="username">
            <p class="field-error">Pick another username.</p>
            <label for="email">Email</label>
            <input id="email" name="email">
            <p class="field-error">That address is not valid.</p>
            <label for="password">Password</label>
            <input id="password" name="password">
        """.trimIndent()
        val result = DoorHtml.doorResult(FormResponse(200, html, null), listOf("check your inbox"))
        assertFalse(result.ok)
        assertEquals("Pick another username.", result.fieldErrors["username"])
        assertEquals("That address is not valid.", result.fieldErrors["email"])
        assertEquals(2, result.fieldErrors.size)
    }

    @Test
    fun acceptsARedirectAsSuccess() {
        val result = DoorHtml.doorResult(FormResponse(302, "", "/verify"), listOf("check your inbox"))
        assertTrue(result.ok)
    }

    @Test
    fun acceptsTheRenderedSuccessMarker() {
        val html = """
            <h1>Check your inbox</h1>
            <p class="form-note">We sent a code to the address you signed up with.</p>
        """.trimIndent()
        val result = DoorHtml.doorResult(FormResponse(200, html, null), listOf("check your inbox"))
        assertTrue(result.ok)
        assertTrue(result.notice.contains("sent a code", ignoreCase = true))
    }

    @Test
    fun doesNotTreatAGeneralErrorAsSuccess() {
        val html = """
            <p class="form-note error">That code did not match.</p>
            <h1>Confirm your email</h1>
        """.trimIndent()
        val result = DoorHtml.doorResult(FormResponse(200, html, null), listOf("email confirmed"))
        assertFalse(result.ok)
        assertEquals("That code did not match.", result.message)
    }

    @Test
    fun stripsMarkupFromMessages() {
        val html = """<p class="form-note error">Too many attempts. <b>Wait a minute.</b></p>"""
        val result = DoorHtml.doorResult(FormResponse(429, html, null), emptyList())
        assertFalse(result.ok)
        assertEquals("Too many attempts. Wait a minute.", result.message)
    }
    @Test
    fun readsEachServerSignupStageWithoutTreatingItAsCreation() {
        for (step in AccountWebApi.SignupStep.entries) {
            val html = """<div data-signup-step="${step.value}" data-masked-email="j…@example.com"><input name="_csrf" value="fresh-token"></div>"""
            val gate = DoorHtml.signupGate(html)!!
            assertEquals(step, gate.step)
            assertEquals("fresh-token", gate.csrf)
            assertEquals("j…@example.com", gate.maskedEmail)
        }
    }

    @Test
    fun readsTheExistingWebsiteStepFieldDuringRollingDeploys() {
        val html = """<form><input name="_csrf" value="csrf"><input name="step" value="code"></form>"""
        assertEquals(AccountWebApi.SignupStep.CODE, DoorHtml.signupGate(html)!!.step)
    }

    @Test
    fun doesNotInferSignupSuccessFromAnUnrelatedPage() {
        assertEquals(null, DoorHtml.signupGate("""<form><input name="_csrf" value="csrf"></form>"""))
    }
}
