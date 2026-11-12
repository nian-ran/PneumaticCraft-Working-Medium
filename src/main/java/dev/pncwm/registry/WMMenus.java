package dev.pncwm.registry;

import dev.pncwm.WorkingMedium;
import dev.pncwm.menu.ApparatusMenu;
import dev.pncwm.menu.CatalogMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WMMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, WorkingMedium.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ApparatusMenu>> APPARATUS =
            MENUS.register("apparatus", () -> IMenuTypeExtension.create((id, inventory, buffer) -> new ApparatusMenu(id, inventory, buffer)));

    public static final DeferredHolder<MenuType<?>, MenuType<CatalogMenu>> CATALOG =
            MENUS.register("catalog", () -> IMenuTypeExtension.create((id, inventory, buffer) ->
                    new CatalogMenu(id, inventory, buffer.readBlockPos())));

    private WMMenus() {
    }
}
