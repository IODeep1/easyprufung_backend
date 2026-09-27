package com.easyprufung.backend;

public class EndPoints {
    public interface Protected {
        String PREFIX = "/api";
    }

    //ADMIN
    public static final String ADMIN_LOGIN = "/public/admin/login";
    public static final String ADMIN_CREATE= "/api/admin/create";
    public static final String ADMIN_DELETE= "/api/admin/delete/{id}";
    public static final String ADMIN_GET = "/api/admin/get";
    public static final String ADMIN_LIST= "/api/admin/list";
    public static final String ADMIN_ROLE_LIST= "/api/admin/role_list";
    public static final String ADMIN_UPDATE= "/api/admin/update";


    //ADMIN
    public static final String CONTACT_CREATE= "/public/contact/create";
    public static final String CONTACT_DELETE= "/api/contact/delete/{id}";
    public static final String CONTACT_GET = "/api/contact/get";
    public static final String CONTACT_LIST= "/api/contact/list";

    //User
    public static final String USER_GOOGLE_LOGIN = "/public/user/google/login";
    public static final String USER_LOGIN = "/public/user/login";
    public static final String USER_CREATE= "/public/user/create";
    public static final String USER_PROJECTS = "/api/user/projects";
    public static final String USER_DELETE= "/api/user/delete/{id}";
    public static final String USER_GET = "/api/user/get";
    public static final String USER_LIST= "/api/user/list";
    public static final String USER_LIST_COUNT= "/api/user/list/count";
    public static final String USER_UPDATE= "/api/user/update";
    public static final String USER_INTERCOM_HASH= "/api/user/intercom-hash";
    public static final String USER_FORGOT_PASSWORD= "/public/user/forgot-password";
    public static final String USER_RESET_PASSWORD= "/public/user/reset-password";



    //Subscription
    public static final String SUBSCRIPTION_CREATE= "/api/subscription/create";
    public static final String SUBSCRIPTION_DELETE= "/api/subscription/delete/{id}";
    public static final String SUBSCRIPTION_GET = "/api/subscription/get";
    public static final String SUBSCRIPTION_LIST= "/api/subscription/list";
    public static final String SUBSCRIPTION__UPDATE= "/api/subscription/update";


    //PromoCode
    public static final String PROMO_CODE_ACTIVATE= "/api/promocode/activate";
    public static final String PROMO_CODE_CREATE= "/api/promocode/create";
    public static final String PROMO_CODE_DELETE= "/api/promocode/delete/{id}";
    public static final String PROMO_CODE_GET = "/api/promocode/get";
    public static final String PROMO_CODE_LIST= "/api/promocode/list";
    public static final String PROMO_CODE_UPDATE= "/api/promocode/update";


    //RESOURCES
    public static final String RESOURCES_IMAGE_UPLOAD= "/api/resources/images/upload";

    //DOMAIN
    public static final String DOMAIN_CHECK ="/api/domain/check";


    public interface Public {
        String PUBLIC ="/public";
        String SERVERINFO ="/public/serverinfo";
    }
}
