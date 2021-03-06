package com.linagora.tmail.james.app;

import java.io.File;
import java.util.Optional;

import org.apache.james.SearchConfiguration;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.server.core.JamesServerResourceLoader;
import org.apache.james.server.core.MissingArgumentException;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.server.core.configuration.FileConfigurationProvider;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.apache.james.utils.PropertiesProvider;

import com.github.fge.lambdas.Throwing;
import com.linagora.tmail.blob.blobid.list.BlobStoreConfiguration;
import com.linagora.tmail.combined.identity.UsersRepositoryModuleChooser;
import com.linagora.tmail.encrypted.MailboxConfiguration;

public class DistributedJamesConfiguration implements Configuration {
    public static class Builder {
        private Optional<MailboxConfiguration> mailboxConfiguration;
        private Optional<SearchConfiguration> searchConfiguration;
        private Optional<BlobStoreConfiguration> blobStoreConfiguration;
        private Optional<String> rootDirectory;
        private Optional<ConfigurationPath> configurationPath;
        private Optional<UsersRepositoryModuleChooser.Implementation> usersRepositoryImplementation;

        private Builder() {
            searchConfiguration = Optional.empty();
            mailboxConfiguration = Optional.empty();
            rootDirectory = Optional.empty();
            configurationPath = Optional.empty();
            blobStoreConfiguration = Optional.empty();
            usersRepositoryImplementation = Optional.empty();
        }

        public Builder workingDirectory(String path) {
            rootDirectory = Optional.of(path);
            return this;
        }

        public Builder workingDirectory(File file) {
            rootDirectory = Optional.of(file.getAbsolutePath());
            return this;
        }

        public Builder useWorkingDirectoryEnvProperty() {
            rootDirectory = Optional.ofNullable(System.getProperty(WORKING_DIRECTORY));
            if (!rootDirectory.isPresent()) {
                throw new MissingArgumentException("Server needs a working.directory env entry");
            }
            return this;
        }

        public Builder configurationPath(ConfigurationPath path) {
            configurationPath = Optional.of(path);
            return this;
        }

        public Builder configurationFromClasspath() {
            configurationPath = Optional.of(new ConfigurationPath(FileSystem.CLASSPATH_PROTOCOL));
            return this;
        }

        public Builder blobStore(BlobStoreConfiguration blobStoreConfiguration) {
            this.blobStoreConfiguration = Optional.of(blobStoreConfiguration);
            return this;
        }

        public Builder mailbox(MailboxConfiguration mailboxConfiguration) {
            this.mailboxConfiguration = Optional.of(mailboxConfiguration);
            return this;
        }

        public Builder searchConfiguration(SearchConfiguration searchConfiguration) {
            this.searchConfiguration = Optional.of(searchConfiguration);
            return this;
        }

        public Builder usersRepository(UsersRepositoryModuleChooser.Implementation implementation) {
            this.usersRepositoryImplementation = Optional.of(implementation);
            return this;
        }

        public DistributedJamesConfiguration build() {
            ConfigurationPath configurationPath = this.configurationPath.orElse(new ConfigurationPath(FileSystem.FILE_PROTOCOL_AND_CONF));
            JamesServerResourceLoader directories = new JamesServerResourceLoader(rootDirectory
                .orElseThrow(() -> new MissingArgumentException("Server needs a working.directory env entry")));

            FileSystemImpl fileSystem = new FileSystemImpl(directories);
            BlobStoreConfiguration blobStoreConfiguration = this.blobStoreConfiguration.orElseGet(Throwing.supplier(
                () -> BlobStoreConfiguration.parse(
                    new PropertiesProvider(fileSystem, configurationPath))));

            SearchConfiguration searchConfiguration = this.searchConfiguration.orElseGet(Throwing.supplier(
                () -> SearchConfiguration.parse(
                    new PropertiesProvider(fileSystem, configurationPath))));

            MailboxConfiguration mailboxConfiguration = this.mailboxConfiguration.orElseGet(Throwing.supplier(
                () -> MailboxConfiguration.parse(
                    new PropertiesProvider(fileSystem, configurationPath))));

            FileConfigurationProvider configurationProvider = new FileConfigurationProvider(fileSystem, Basic.builder()
                .configurationPath(configurationPath)
                .workingDirectory(directories.getRootDirectory())
                .build());
            UsersRepositoryModuleChooser.Implementation usersRepositoryChoice = usersRepositoryImplementation.orElseGet(
                () -> UsersRepositoryModuleChooser.Implementation.parse(configurationProvider));

            return new DistributedJamesConfiguration(
                configurationPath,
                directories,
                mailboxConfiguration,
                blobStoreConfiguration,
                searchConfiguration,
                usersRepositoryChoice);
        }
    }

    public static DistributedJamesConfiguration.Builder builder() {
        return new Builder();
    }

    private final ConfigurationPath configurationPath;
    private final JamesDirectoriesProvider directories;
    private final MailboxConfiguration mailboxConfiguration;
    private final BlobStoreConfiguration blobStoreConfiguration;
    private final SearchConfiguration searchConfiguration;
    private final UsersRepositoryModuleChooser.Implementation usersRepositoryImplementation;

    public DistributedJamesConfiguration(ConfigurationPath configurationPath, JamesDirectoriesProvider directories, MailboxConfiguration mailboxConfiguration, BlobStoreConfiguration blobStoreConfiguration, SearchConfiguration searchConfiguration, UsersRepositoryModuleChooser.Implementation usersRepositoryImplementation) {
        this.configurationPath = configurationPath;
        this.directories = directories;
        this.mailboxConfiguration = mailboxConfiguration;
        this.blobStoreConfiguration = blobStoreConfiguration;
        this.searchConfiguration = searchConfiguration;
        this.usersRepositoryImplementation = usersRepositoryImplementation;
    }

    @Override
    public ConfigurationPath configurationPath() {
        return configurationPath;
    }

    @Override
    public JamesDirectoriesProvider directories() {
        return directories;
    }

    public BlobStoreConfiguration blobStoreConfiguration() {
        return blobStoreConfiguration;
    }

    public SearchConfiguration searchConfiguration() {
        return searchConfiguration;
    }

    public MailboxConfiguration mailboxConfiguration() {
        return mailboxConfiguration;
    }

    public UsersRepositoryModuleChooser.Implementation getUsersRepositoryImplementation() {
        return usersRepositoryImplementation;
    }
}
