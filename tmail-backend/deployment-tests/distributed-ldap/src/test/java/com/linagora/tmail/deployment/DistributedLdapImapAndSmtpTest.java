package com.linagora.tmail.deployment;

import org.apache.james.mpt.imapmailbox.external.james.host.external.ExternalJamesConfiguration;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;

public class DistributedLdapImapAndSmtpTest extends ImapAndSmtpContract {
    @RegisterExtension
    TmailDistributedLdapExtension extension = new TmailDistributedLdapExtension();

    @Override
    protected ExternalJamesConfiguration configuration() {
        return extension.configuration();
    }

    @Override
    protected GenericContainer<?> container() {
        return extension.getContainer();
    }
}
