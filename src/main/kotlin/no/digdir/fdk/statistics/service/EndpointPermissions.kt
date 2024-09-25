package no.digdir.fdk.statistics.service

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

private const val ROLE_ROOT_ADMIN = "system:root:admin"

@Component
class EndpointPermissions {

    fun hasAdminPermission(jwt: Jwt): Boolean {
        val authorities: String? = jwt.claims["authorities"] as? String

        return authorities?.contains(ROLE_ROOT_ADMIN) ?: false
    }

}
