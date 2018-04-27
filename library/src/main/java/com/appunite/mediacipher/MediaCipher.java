package com.appunite.mediacipher;


import android.app.Activity;
import android.content.Context;

import com.appunite.mediacipher.crypto.AESCrypter;
import com.appunite.mediacipher.crypto.AESCrypterBelowM;
import com.appunite.mediacipher.crypto.AESCrypterMPlus;
import com.appunite.mediacipher.crypto.download.EncryptingDownloader;
import com.appunite.mediacipher.crypto.exoplayer.EncryptedFileDataSourceFactory;
import com.appunite.mediacipher.helpers.Checker;
import com.appunite.mediacipher.helpers.VersionsUtils;
import com.google.android.exoplayer2.upstream.DataSource;

import java.io.File;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;


public final class MediaCipher {

    private static volatile MediaCipher singleton;

    @Nonnull private final Listener listener;
    @Nonnull private final Config config;
    @Nonnull private final AESCrypter aesCrypter;

    public static MediaCipher init(@Nonnull final Context applicationContext,
                                   @Nonnull final Listener listener) {
        return init(applicationContext, new Config(), listener);
    }

    public static MediaCipher init(@Nonnull final Context applicationContext,
                                   @Nonnull final Config config,
                                   @Nonnull final Listener listener) {
        Checker.checkArgument(!(applicationContext instanceof Activity), "You have to pass application context instead of activity.");
        Checker.checkArgument(listener != null, "You have to pass Listener to handle keystore error. More in README");

        if (singleton == null) {
            synchronized (MediaCipher.class) {
                if (singleton == null) {
                    singleton = new MediaCipher(applicationContext, config, listener);
                    singleton.internalInit();
                }
            }
        }

        return singleton;
    }

    private MediaCipher(@Nonnull final Context context,
                        @Nonnull final Config config,
                        @Nonnull final Listener listener) {
        this.config = config;
        this.listener = listener;
        final KeysPreferences keysPreferences = new KeysPreferences(context);
        this.aesCrypter = VersionsUtils.isAtLeastMarshMallow() ?
                new AESCrypterMPlus(context, keysPreferences) : new AESCrypterBelowM(context, keysPreferences);
        aesCrypter.setListener(listener);
    }

    private void internalInit() {
        aesCrypter.init();
    }

    private static void checkInitialized() {
        Checker.checkArgument(singleton != null, "You must call init(...) method first.");
    }

    @Nonnull
    public EncryptingDownloader getEncryptingDownloader(@Nonnull OkHttpClient okHttpClient) {
        return new EncryptingDownloader(okHttpClient, aesCrypter, listener);
    }

    @Nonnull
    public DataSource.Factory getEncryptedFileDataSourceFactory(final File file) throws Exception {
        return new EncryptedFileDataSourceFactory(aesCrypter.getDecryptingKeys(file));
    }

    @Nonnull
    public static MediaCipher getInstance() {
        checkInitialized();
        return singleton;
    }

    @Nonnull
    public Config getConfig() {
        return config;
    }
}
