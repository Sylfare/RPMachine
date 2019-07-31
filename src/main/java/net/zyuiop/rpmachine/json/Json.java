package net.zyuiop.rpmachine.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.zyuiop.rpmachine.economy.shops.AbstractShopSign;
import net.zyuiop.rpmachine.permissions.DelegatedPermission;

/**
 * @author Louis Vialar
 */
public class Json {
    public static final Gson GSON =
            new GsonBuilder()
                    .registerTypeHierarchyAdapter(AbstractShopSign.class, new ShopSerializer())
                    .registerTypeHierarchyAdapter(DelegatedPermission.class, new PermissionSerializer())
                    .create();

}
