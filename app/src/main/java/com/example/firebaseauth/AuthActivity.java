package com.example.firebaseauth;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {

    private enum Provider { BASIC, GOOGLE_AUTH }
    GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 007;
    private static final String TAG = "GoogleAuth";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setup() {
        this.setTitle("Authentication");
        Button signUp = (Button) findViewById(R.id.logOut);
        signUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String email = ((EditText) findViewById(R.id.email)).getText().toString();
                String password = ((EditText) findViewById(R.id.password)).getText().toString();
                if(!email.isEmpty() && !password.isEmpty()) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>(){
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                showAlert("success");
                            } else {
                                showAlert("error");
                            }
                        }
                    });
                }
            }
        });

        Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String email = ((EditText) findViewById(R.id.email)).getText().toString();
                String password = ((EditText) findViewById(R.id.password)).getText().toString();
                if(!email.isEmpty() && !password.isEmpty()) {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                            public void onComplete(Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    showHome(email, AuthActivity.Provider.BASIC);
                                } else {
                                    showAlert("errorLogin");
                                }
                        }
                    });
                }
            }
        });

        SignInButton googleSignIn = findViewById(R.id.googleLogin);
        googleSignIn.setSize(SignInButton.SIZE_STANDARD);
        googleSignIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            }
        });
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            // Google Sign In was successful, authenticate with Firebase
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                            showHome(task.getResult().getEmail(), AuthActivity.Provider.BASIC);
                        } catch (ApiException e) {
                            // Google Sign In failed, update UI appropriately
                            Log.w(TAG, "Google sign in failed", e);
                        }
                    }
                }
            });

    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(message.equalsIgnoreCase("error")) {
            builder.setTitle("Error");
            builder.setMessage("An error has occurred while trying to register the user");
        } else if (message.equalsIgnoreCase("errorLogin")){
            builder.setTitle("Error");
            builder.setMessage("An error has occurred while trying to login");
        } else if (message.equalsIgnoreCase("errorLoginGoogle")){
            builder.setTitle("Error");
            builder.setMessage("An error has occurred while trying to login with Google");
        } else {
            builder.setTitle("Success!");
            builder.setMessage("User successfully registered. Please login.");
        }
        builder.setPositiveButton("Accept", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showHome(String email, Provider provider) {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("provider", provider.name());
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null) {
            showHome(account.getEmail(), AuthActivity.Provider.BASIC);
        } else {
            setup();
        }
    }
}