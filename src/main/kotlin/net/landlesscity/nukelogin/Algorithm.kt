// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin

import com.password4j.Argon2Function
import com.password4j.BcryptFunction
import com.password4j.HashingFunction
import com.password4j.SaltGenerator
import com.password4j.types.Argon2
import main.sqlite.Passwords_current.Hash

/**
 * ## NukeLogin algorithms
 * Lifecycle of algorithms for NukeLogin can be described as such:
 *
 * 1. New algorithm appear at the [RECOMMENDED] stage.
 *
 * 2. If some tolerable weakness or concerns were found in algorithm,
 * move it to the [SOON_DEPRECATED] stage, to give some more time for
 * server owners to react (help by printing warning). After some time,
 * move it to [DEPRECATED].
 * Old users are migrated incrementally when they log in.
 *
 * 3. If some serious weakness was discovered in algorithm,
 * which warrants stopping its usage as soon as possible,
 * move it straight to the [DEPRECATED] stage, so that admins
 * can secure credentials of new users as soon as possible.
 * Old users are migrated incrementally when they log in.
 *
 * ## Algorithms for other plugins.
 * They go straight to the [IMPORTED_AND_DEPRECATED].
 * Old users are migrated incrementally when they log in.
 */
internal enum class LifecyclePolicy {
    /**
     * Recommended algorithm.
     *
     * * Can be set as default in config.
     * * Allowed in all situations: registration, login etc.
     * * Parameters are fixed, and correspond to unique algorithm name in database.
     *
     * @see [Algorithm.ARGON2ID_OWASP_2025]
     */
    RECOMMENDED,

    /**
     * Algorithm which might be deprecated soon.
     *
     * * Can be set as default in config, but it will trigger warning with time notice.
     * * Allowed in all situations: registration, login etc.
     * * Parameters are fixed, and correspond to unique algorithm name in database.
     */
    SOON_DEPRECATED,

    /**
     * Legacy algorithm for old passwords inherited from old versions.
     *
     * * Can not be set as default in config, it will trigger error.
     * * Allowed only for login.
     * * Must be updated to [RECOMMENDED] or [SOON_DEPRECATED] algorithm immediately after that.
     * * Parameters are fixed, and correspond to unique algorithm name in database.
     */
    DEPRECATED,

    /**
     * Legacy algorithm for old passwords imported from other plugins.
     *
     * * Can not be set as default in config, it will trigger error.
     * * Allowed only for login.
     * * Must be updated to [RECOMMENDED] or [SOON_DEPRECATED] algorithm immediately after that.
     * * Parameters are derived from existing hash.
     *
     * @see [Algorithm.BCRYPT_USERLOGIN_2022]
     */
    IMPORTED_AND_DEPRECATED;

    internal val allowsRegistration: Boolean
        get() = when (this) {
            RECOMMENDED, SOON_DEPRECATED -> true
            DEPRECATED, IMPORTED_AND_DEPRECATED -> false
        }

    internal val warrantsUpdate: Boolean
        get() = when (this) {
            RECOMMENDED, SOON_DEPRECATED -> false
            DEPRECATED, IMPORTED_AND_DEPRECATED -> true
        }
}

/**
 * Cryptographic password algorithm versioned specifications
 *
 * ## What is Versioned Specification
 * Each enum tag represents a "versioned specification", which means combination of:
 *   - Cryptographic parameters (memory, iterations, parallelism).
 *   - Salt length.
 *   - Hashing function instance.
 *
 * Versioned Specification is immutable and can not be modified. To change any logic
 * or parameters here requires creating new enum entry.
 *
 * ## Storing in database
 * Each enum entry is binded to the database by using `.name` property.
 * It serves as a permanent and unique identifier.
 *
 * Here's format: `{algorithm_name}_{origin}_{year}`.
 * Optionally it can also have `_{revision}`. Everything is in UPPERCASE.
 *
 * Example: `ARGON2ID_OWASP_2025` (Argon2id implemented with parameters recommended on OWASP website in 2025)
 *
 * ## Backward and Forward Compatibility
 * I'm focusing more on backward compatibiltity for now (from older to newer).
 *
 * Downgrade (from newer to older) is not supported: plugin may throw an error and shutdown itself,
 * if it finds some algorithm in database which it can't recognize (mostly for security reasons).
 *
 * Updates are supported: for now I plan to never remove old entries, so that users
 * with passwords hashed using deprecated algorithms would be able to log in
 * (they will be updated to new algorithm after that).
 *
 * @see LifecyclePolicy
 *
 * @property[policy] Lifecycle policy.
 * It dictates allowed actions, stability of parameters,
 * and should there be any migrations to new algorithms.
 *
 * @property[saltLen] Salt length (in bytes), used for registrations.
 * Always 0 for deprecated algorithm.
 *
 * @property[hasher] Instance for hashing and verifying passwords.
 */
internal enum class Algorithm(
    internal val policy: LifecyclePolicy,
    private val saltLen: Int,
    private val hasher: (Hash?) -> HashingFunction
) {
    @Suppress("MaxLineLength", "MagicNumber")
    /**
     * Default [Algorithm] for new passwords.
     *
     * Uses [recommendations from OWASP site](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#argon2id),
     * last checked for 2025 year.
     */
    ARGON2ID_OWASP_2025(
        policy = LifecyclePolicy.RECOMMENDED,
        saltLen = 24,
        hasher = { _ ->
            val memory = 19_456
            val iterations = 2
            val parallelism = 1
            val outputLength = 32
            val type = Argon2.ID
            val version = 19
            Argon2Function.getInstance(
                memory,
                iterations,
                parallelism,
                outputLength,
                type,
                version,
            )!!
        }
    ),

    /**
     * [Algorithm] for passwords which were migrated
     * from UserLogin plugin, of version 2.12 or somewhere near.
     *
     * Sources that confirm it uses BCrypt:
     * * [pom.xml](https://github.com/Grazen0/UserLogin/blob/e7639d0f1980150b6217aaf70e4f80722331b28d/pom.xml#L116-L120)
     * * [Database.java](https://github.com/Grazen0/UserLogin/blob/e7639d0f1980150b6217aaf70e4f80722331b28d/src/main/java/com/elchologamer/userlogin/database/Database.java#L20)
     */
    BCRYPT_USERLOGIN_2022(
        policy = LifecyclePolicy.IMPORTED_AND_DEPRECATED,
        saltLen = 0,
        hasher = { hash ->
            requireNotNull(hash) { "BCryptA infers parameters from existing hash" }
            BcryptFunction.getInstanceFromHash(hash.hash.toString(Charsets.UTF_8))
        }
    );

    /**
     * Only "algorithm which was not deprecated" can be used.
     *
     * @param[plainText] Password from `/register` or `/changepassword`
     * @return New hash, with config and salt inside.
     */
    internal fun hash(plainText: ByteArray): Hash {
        check(policy.allowsRegistration) {
            "Algorithm ${name} is not allowed for new passwords, reason: ${policy.name}"
        }

        val salt = SaltGenerator.generate(saltLen)
        val hash = hasher(null).hash(plainText, salt)
        return Hash(hash.bytes)
    }

    /**
     * Any algorithm can be used. If algorithm is deprecated,
     * update after that to default algorithm immediately.
     *
     * @param[plainText] Password from `/login`
     * @param[hash] Hash from database.
     * @return `true` if password matches, otherwise `false`.
     */
    internal fun verify(plainText: ByteArray, hash: Hash): Boolean {
        return hasher(hash).check(plainText, hash.hash)
    }

    internal companion object {
        /** TODO move to config */
        internal val Default: Algorithm = ARGON2ID_OWASP_2025

        /** Safe alternative to `valueOf` which returns `null` on mismatch. */
        internal fun decode(databaseValue: String): Algorithm? = try {
            valueOf(databaseValue)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
