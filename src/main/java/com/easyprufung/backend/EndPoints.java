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

    //Project
    public static final String PROJECT_DELETE= "/api/project/delete";
    public static final String PROJECT_GET = "/api/project/get";
    public static final String PROJECT_VALIDATE = "/api/project/validate";
    public static final String PROJECT_SAVE_DESCRIPTION = "/api/project/description/save";
    public static final String PROJECT_GENERATE_NAME = "/api/project/name/generate";
    public static final String PROJECT_SAVE_NAME = "/api/project/name/save";
    public static final String PROJECT_LANDINGPAGE_SAVE = "/api/project/landingpage/save";
    public static final String PROJECT_LANDINGPAGE_GET = "/api/project/landingpage/get";
    public static final String PROJECT_LANDINGPAGE_CREATE = "/api/project/landingpage/create";
    public static final String PROJECT_LANDINGPAGE_DELETE = "/api/project/landingpage/delete";
    public static final String PROJECT_LANDINGPAGE_DOWNLOAD = "/api/project/landingpage/download";
    public static final String PROJECT_SOCIAL_CHECK = "/api/project/social-check";
    public static final String PROJECT_DOMAIN_CHECK = "/api/project/domain-check";
    public static final String PROJECT_LOGO_SVG_CONTENT= "/api/project/logo/svg-content";
    public static final String PROJECT_LOGO_REMOVE_BG = "/api/project/logo/removebg";
    public static final String PROJECT_LOGO_SAVE = "/api/project/logo/save";
    public static final String PROJECT_LOGO_DELETE = "/api/project/logo/delete";
    public static final String PROJECT_LOGO_GENERATE = "/api/project/logo/generate";
    public static final String PROJECT_LIST_COUNT= "/api/project/list/count";
    public static final String PROJECT_LIST= "/api/project/list";
    public static final String PROJECT_CREATE= "/api/project/create";
    public static final String PROJECT_PREBUILT_CREATE= "/api/project/prebuilt/create";
    public static final String PROJECT_UPDATE= "/api/project/update";
    public static final String PROJECT_UPVOTE= "/api/project/upvote";
    public static final String PROJECT_DOMAIN_ADD = "/api/project/domain/add";
    public static final String PROJECT_DOMAIN_DELETE = "/api/project/domain/delete";
    public static final String PROJECT_WAITLIST_ADD = "/public/project/waitlist/add";
    public static final String PROJECT_CONTACTFORM_ADD = "/public/project/contactform/add";
    public static final String PROJECT_WAITLIST_LIST = "/api/project/waitlist/list";

    public static final String PROJECT_PUBLIC_LIST= "/public/project/public/list";

    //RESOURCES
    public static final String RESOURCES_IMAGE_UPLOAD= "/api/resources/images/upload";
    public static final String RESOURCES_IMAGE_GET= "/public/resources/images/{imageName:.+}";
    public static final String RESOURCES_IMAGE_DELETE= "/public/resources/images/delete/{imageName:.+}";

    //TOOLS
    public static final String TOOL_IMAGE_REMOVE_BG = "/public/tool/image/removebg";

    //DOMAIN
    public static final String DOMAIN_CHECK ="/api/domain/check";

    //Chat
    public static final String CHAT_PROJECT_HISTORY= "/api/chat/project/history";
    public static final String CHAT_PROJECT_VALIDATION= "/api/chat/project/validation";
    public static final String CHAT_PROJECT_NAME= "/api/chat/project/name";
    public static final String CHAT_PROJECT_ICON= "/api/chat/project/icon";
    public static final String CHAT_PROJECT_LANDINGPAGE= "/api/chat/project/landingpage";

    public interface Public {
        String PUBLIC ="/public";
        String SERVERINFO ="/public/serverinfo";
    }
}
