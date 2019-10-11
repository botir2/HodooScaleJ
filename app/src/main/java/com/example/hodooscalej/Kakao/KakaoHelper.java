package com.example.hodooscalej.Kakao;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.example.hodooscalej.R;
import com.example.hodooscalej.AddPet;
import com.example.hodooscalej.MainActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.KakaoSDK;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.facebook.FacebookSdk.getApplicationContext;

public class KakaoHelper {

    private String TAG = "MainActivity";
    private MainActivity mListener;

    private KakaoHelperListener mklistener;
    FirebaseAuth firebaseAuth;
    private static String urlVal;


    public KakaoHelper(MainActivity mainActivity) {

        mListener =  mainActivity;
        firebaseAuth = FirebaseAuth.getInstance();

        urlVal = mListener.getResources().getString(R.string.kakao_validation_server_domain); // verifyToken";

        Session.getCurrentSession().addCallback(new KakaoSessionCallback());

    }

    public void performSignIn() {

        final List<AuthType> authTypes = getAuthTypes();
        if (authTypes.size() == 1) {
            Session.getCurrentSession().open(authTypes.get(0), mListener);
        } else {
            final Item[] authItems = createAuthItemArray(authTypes);
            ListAdapter adapter = createLoginAdapter(authItems);
            final Dialog dialog = createLoginDialog(authItems, adapter);
            dialog.show();
        }
    }

    private static class Item {
        final int textId;
        public final int icon;
        final int contentDescId;
        final AuthType authType;
        Item(final int textId, final Integer icon, final int contentDescId, final AuthType authType) {
            this.textId = textId;
            this.icon = icon;
            this.contentDescId = contentDescId;
            this.authType = authType;
        }
    }


    private List<AuthType> getAuthTypes() {
        final List<AuthType> availableAuthTypes = new ArrayList<>();
        if (Session.getCurrentSession().getAuthCodeManager().isTalkLoginAvailable()) {
            availableAuthTypes.add(AuthType.KAKAO_TALK);
        }
        if (Session.getCurrentSession().getAuthCodeManager().isStoryLoginAvailable()) {
            availableAuthTypes.add(AuthType.KAKAO_STORY);
        }
        availableAuthTypes.add(AuthType.KAKAO_ACCOUNT);

        AuthType[] authTypes = KakaoSDK.getAdapter().getSessionConfig().getAuthTypes();
        if (authTypes == null || authTypes.length == 0 || (authTypes.length == 1 && authTypes[0] == AuthType.KAKAO_LOGIN_ALL)) {
            authTypes = AuthType.values();
        }
        availableAuthTypes.retainAll(Arrays.asList(authTypes));

        // 개발자가 설정한 것과 available 한 타입이 없다면 직접계정 입력이 뜨도록 한다.
        if(availableAuthTypes.size() == 0){
            availableAuthTypes.add(AuthType.KAKAO_ACCOUNT);
        }
        return availableAuthTypes;
    }

    private Item[] createAuthItemArray(final List<AuthType> authTypes) {
        final List<Item> itemList = new ArrayList<Item>();
        if(authTypes.contains(AuthType.KAKAO_TALK)) {
            itemList.add(new Item(com.kakao.usermgmt.R.string.com_kakao_kakaotalk_account, com.kakao.usermgmt.R.drawable.talk, com.kakao.usermgmt.R.string.com_kakao_kakaotalk_account_tts, AuthType.KAKAO_TALK));
        }
        if(authTypes.contains(AuthType.KAKAO_STORY)) {
            itemList.add(new Item(com.kakao.usermgmt.R.string.com_kakao_kakaostory_account, com.kakao.usermgmt.R.drawable.story, com.kakao.usermgmt.R.string.com_kakao_kakaostory_account_tts, AuthType.KAKAO_STORY));
        }
        if(authTypes.contains(AuthType.KAKAO_ACCOUNT)){
            itemList.add(new Item(com.kakao.usermgmt.R.string.com_kakao_other_kakaoaccount, com.kakao.usermgmt.R.drawable.account, com.kakao.usermgmt.R.string.com_kakao_other_kakaoaccount_tts, AuthType.KAKAO_ACCOUNT));
        }

        return itemList.toArray(new Item[itemList.size()]);
    }

    @SuppressWarnings("deprecation")
    private ListAdapter createLoginAdapter(final Item[] authItems) {
    /*
        Prepare to show the user the possible auth types..
     */
        return new ArrayAdapter<Item>(
                mListener,
                android.R.layout.select_dialog_item,
                android.R.id.text1, authItems){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(com.kakao.usermgmt.R.layout.layout_login_item, parent, false);
                }
                ImageView imageView = convertView.findViewById(com.kakao.usermgmt.R.id.login_method_icon);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imageView.setImageDrawable(mListener.getResources().getDrawable(authItems[position].icon, getContext().getTheme()));
                } else {
                    imageView.setImageDrawable(mListener.getResources().getDrawable(authItems[position].icon));
                }
                TextView textView = convertView.findViewById(com.kakao.usermgmt.R.id.login_method_text);
                textView.setText(authItems[position].textId);
                return convertView;
            }
        };
    }

    /**
     *
     * Create a dialog object that will be actually displayed to the user.
     * @param authItems Item array containing information of possible AuthTypes
     * @param adapter Dialog의 list view에 Used adapter
     * @return Pop-up shows login methods dialog
     */
    private Dialog createLoginDialog(final Item[] authItems, final ListAdapter adapter) {
        final Dialog dialog = new Dialog(mListener, com.kakao.usermgmt.R.style.LoginDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(com.kakao.usermgmt.R.layout.layout_login_dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

//        TextView textView = (TextView) dialog.findViewById(R.id.login_title_text);
//        Typeface customFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/KakaoOTFRegular.otf");
//        if (customFont != null) {
//            textView.setTypeface(customFont);
//        }

        ListView listView = dialog.findViewById(com.kakao.usermgmt.R.id.login_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final AuthType authType = authItems[position].authType;
                if (authType != null) {
                    openSession(authType);
                }
                dialog.dismiss();
            }
        });

        Button closeButton = dialog.findViewById(com.kakao.usermgmt.R.id.login_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        return dialog;
    }

    public void openSession(final AuthType authType) {
        Session.getCurrentSession().open(authType, mListener);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            Log.d(TAG, "requestCode___________________: " + requestCode + ", resultCode: " + resultCode);
            return;
        }

       /* if(requestCode == RC_SIGN_IN){

            String credential = Session.getCurrentSession().getAccessToken();
            firebaseAuthWithGoogle(credential);
        }*/


    }

    public void requestMe() {
        Log.d(TAG, "requestMe");
        UserManagement.getInstance().requestMe(new MeResponseCallback() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                String message = "failed to get user info. msg=" + errorResult;
                Log.e(TAG, message);
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                Log.e(TAG, "seesionClosed");
            }

            @Override
            public void onSuccess(UserProfile userProfile) {
                Toast.makeText(mListener, "userProfile!" + userProfile.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNotSignedUp() {
                Log.e(TAG, "NotSignedUp");
            }
        });
    }


    /**
     * Update UI based on Firebase's current user. Show Login Button if not logged in.
     */
    private void updateUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null){ AddPetACtivity();  Toast.makeText(mListener, "Login with!" + currentUser, Toast.LENGTH_SHORT).show(); }
        else { Toast.makeText(mListener,"Failed reginstarion: " +currentUser, Toast.LENGTH_LONG).show();}
    }


    /**
     *
     * @param kakaoAccessToken Access token retrieved after successful Kakao Login
     * @return Task object that will call validation server and retrieve firebase token
     */
    private Task<String> getFirebaseJwt(final String kakaoAccessToken) {
        final TaskCompletionSource<String> source = new TaskCompletionSource<>();

        RequestQueue queue = Volley.newRequestQueue(mListener);
        HashMap<String, String> validationObject = new HashMap<>();
        validationObject.put("token", kakaoAccessToken);


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, urlVal, new JSONObject(validationObject), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String firebaseToken = response.getString("firebase_token");
                    source.setResult(firebaseToken);
                } catch (Exception e) {
                    source.setException(e);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
                source.setException(error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("token", kakaoAccessToken);
                return params;
            }
        };

        queue.add(request);
        return source.getTask();
    }

    /**
     * Session callback class for Kakao Login. OnSessionOpened() is called after successful login.
     */
    private class KakaoSessionCallback implements ISessionCallback {
        @Override
        public void onSessionOpened() {

            Toast.makeText(getApplicationContext(), "Successfully logged in to Kakao. Now creating or updating a Firebase User.", Toast.LENGTH_LONG).show();
            String accessToken = Session.getCurrentSession().getAccessToken();
            getFirebaseJwt(accessToken).continueWithTask(new Continuation<String, Task<AuthResult>>() {
                @Override
                public Task<AuthResult> then(@NonNull Task<String> task) throws Exception {
                    String firebaseToken = task.getResult();
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    return auth.signInWithCustomToken(firebaseToken);
                }
            }).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        updateUI();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to create a Firebase user.", Toast.LENGTH_LONG).show();
                        if (task.getException() != null) {
                            Log.e(TAG, task.getException().toString());
                        }
                    }
                }
            });
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if (exception != null) {
                Log.e(TAG, exception.toString());
            }
        }
    }




    public void AddPetACtivity() {
        /*mListener.startActivity(new Intent( mListener, AddPet.class));
        mListener.finish();//<- IMPORTANT*/
        Intent intencion = new Intent(mListener, AddPet.class);
        mListener.startActivity(intencion);
    }

}
