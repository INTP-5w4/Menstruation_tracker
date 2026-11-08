package com.example.andrs.data.sync;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.ClearTokenRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Autorización OAuth para Google Drive con el alcance mínimo {@code drive.appdata}: la app solo
 * puede ver su propia carpeta oculta, nunca los archivos del usuario.
 */
public class DriveAuthManager {
    public static final String SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata";
    private static final List<Scope> SCOPES = Collections.singletonList(new Scope(SCOPE_APPDATA));

    /** La autorización necesita interacción del usuario (pantalla de consentimiento). */
    public static class ConsentRequiredException extends Exception {
        public ConsentRequiredException() {
            super("Se requiere autorizar Google Drive");
        }
    }

    private final AuthorizationClient client;

    public DriveAuthManager(Context context) {
        this.client = Identity.getAuthorizationClient(context.getApplicationContext());
    }

    public AuthorizationRequest buildRequest() {
        return AuthorizationRequest.builder().setRequestedScopes(SCOPES).build();
    }

    /** Para la UI: devuelve un Task cuyo resultado puede requerir resolver un PendingIntent. */
    public Task<AuthorizationResult> authorize() {
        return client.authorize(buildRequest());
    }

    public AuthorizationResult resultFromIntent(@Nullable Intent data) throws ApiException {
        return client.getAuthorizationResultFromIntent(data);
    }

    /** Para segundo plano: obtiene un token sin UI o lanza {@link ConsentRequiredException}. */
    public String getAccessTokenBlocking() throws Exception {
        AuthorizationResult result = Tasks.await(client.authorize(buildRequest()), 30, TimeUnit.SECONDS);
        if (result.hasResolution() || result.getAccessToken() == null) {
            throw new ConsentRequiredException();
        }
        return result.getAccessToken();
    }

    /** Invalida un token en caché (p. ej. tras un 401). */
    public void clearTokenBlocking(String token) {
        try {
            Tasks.await(client.clearToken(ClearTokenRequest.builder().setToken(token).build()),
                    15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // No crítico.
        }
    }

    /** Revoca el acceso de la app a Drive para la cuenta autorizada actualmente. */
    public void revokeBlocking() {
        try {
            AuthorizationResult result = Tasks.await(client.authorize(buildRequest()), 30, TimeUnit.SECONDS);
            if (result.getAccessToken() != null) {
                clearTokenBlocking(result.getAccessToken());
            }
            if (!result.hasResolution() && result.toGoogleSignInAccount() != null) {
                Account account = result.toGoogleSignInAccount().getAccount();
                if (account != null) {
                    Tasks.await(client.revokeAccess(RevokeAccessRequest.builder()
                            .setAccount(account).setScopes(SCOPES).build()), 30, TimeUnit.SECONDS);
                }
            }
        } catch (Exception ignored) {
            // Si falla, el usuario puede revocar desde myaccount.google.com.
        }
    }
}
