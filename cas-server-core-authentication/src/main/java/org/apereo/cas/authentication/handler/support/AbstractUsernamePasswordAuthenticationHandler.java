package org.apereo.cas.authentication.handler.support;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.HandlerResult;
import org.apereo.cas.authentication.PreventedException;
import org.apereo.cas.authentication.UsernamePasswordCredential;
import org.apereo.cas.authentication.handler.NoOpPrincipalNameTransformer;
import org.apereo.cas.authentication.handler.PrincipalNameTransformer;
import org.apereo.cas.authentication.support.PasswordPolicyConfiguration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;
import java.security.GeneralSecurityException;

/**
 * Abstract class to override supports so that we don't need to duplicate the
 * check for UsernamePasswordCredential.
 *
 * @author Scott Battaglia
 * @author Marvin S. Addison
 * @since 3.0.0
 */
public abstract class AbstractUsernamePasswordAuthenticationHandler extends
    AbstractPreAndPostProcessingAuthenticationHandler {

    /**
     * PasswordEncoder to be used by subclasses to encode passwords for
     * comparing against a resource.
     */
    private PasswordEncoder passwordEncoder = NoOpPasswordEncoder.getInstance();
    
    private PrincipalNameTransformer principalNameTransformer = new NoOpPrincipalNameTransformer();

    /** The password policy configuration to be used by extensions. */
    private PasswordPolicyConfiguration passwordPolicyConfiguration;

    @Override
    protected HandlerResult doAuthentication(final Credential credential)
            throws GeneralSecurityException, PreventedException {
        
        final UsernamePasswordCredential userPass = (UsernamePasswordCredential) credential;
        
        if (StringUtils.isBlank(userPass.getUsername())) {
            throw new AccountNotFoundException("Username is null.");
        }
        
        final String transformedUsername = this.principalNameTransformer.transform(userPass.getUsername());
        if (StringUtils.isBlank(transformedUsername)) {
            throw new AccountNotFoundException("Transformed username is null.");
        }

        if (StringUtils.isBlank(userPass.getPassword())) {
            throw new FailedLoginException("Password is null.");
        }
        
        final String transformedPsw = this.passwordEncoder.encode(userPass.getPassword());
        if (StringUtils.isBlank(transformedPsw)) {
            throw new AccountNotFoundException("Encoded password is null.");
        }
        
        userPass.setUsername(transformedUsername);
        userPass.setPassword(this.passwordEncoder.encode(userPass.getPassword()));
        
        return authenticateUsernamePasswordInternal(userPass);
    }

    /**
     * Authenticates a username/password credential by an arbitrary strategy.
     *
     * @param transformedCredential the credential object bearing the transformed username and password.
     *
     * @return HandlerResult resolved from credential on authentication success or null if no principal could be resolved
     * from the credential.
     *
     * @throws GeneralSecurityException On authentication failure.
     * @throws PreventedException On the indeterminate case when authentication is prevented.
     */
    protected abstract HandlerResult authenticateUsernamePasswordInternal(UsernamePasswordCredential transformedCredential)
            throws GeneralSecurityException, PreventedException;
        
    protected PasswordPolicyConfiguration getPasswordPolicyConfiguration() {
        return this.passwordPolicyConfiguration;
    }

    public void setPasswordEncoder(final PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void setPrincipalNameTransformer(final PrincipalNameTransformer principalNameTransformer) {
        this.principalNameTransformer = principalNameTransformer;
    }

    public void setPasswordPolicyConfiguration(final PasswordPolicyConfiguration passwordPolicyConfiguration) {
        this.passwordPolicyConfiguration = passwordPolicyConfiguration;
    }

    @Override
    public boolean supports(final Credential credential) {
        return credential instanceof UsernamePasswordCredential;
    }
}
