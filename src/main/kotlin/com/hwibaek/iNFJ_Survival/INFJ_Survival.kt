package com.hwibaek.iNFJ_Survival

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.roundToInt

class INFJ_Survival : JavaPlugin(), Listener {
    
    lateinit var orb: ItemStack
    lateinit var recipe: ShapedRecipe
    
    private fun registerItemStack() {
        var item = ItemStack.of(Material.ECHO_SHARD)
        var meta = item.itemMeta
        
        meta.displayName(Component.text("무지개 반사", NamedTextColor.AQUA))
        meta.setEnchantmentGlintOverride(true)
        meta.lore(mutableListOf<Component>(Component.text("반사 피해를 일정량 방어해줍니다.", NamedTextColor.LIGHT_PURPLE), Component.text("내구도 : ${config.getInt("orb_durability")} / ${config.getInt("orb_durability")}", NamedTextColor.YELLOW)))
        meta.persistentDataContainer.set(NamespacedKey(this, "durability"), PersistentDataType.INTEGER, config.getInt("orb_durability"))
        meta.setMaxStackSize(1)
        
        item.itemMeta = meta
        orb = item
    }
    
    private fun registerRecipe() {
        recipe = ShapedRecipe(NamespacedKey(this, "orb_recipe"), orb)
        recipe.shape("!@!", "!!!", " ! ")
        recipe.setIngredient('@', Material.DIAMOND)
        recipe.setIngredient('!', Material.IRON_INGOT)
        
        Bukkit.addRecipe(recipe)
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        this.saveDefaultConfig()
        registerItemStack()
        registerRecipe()
        logger.info("INFJ survival enabled!")
    }

    override fun onDisable() {
        logger.info("INFJ survival disabled!")
    }
    
    @EventHandler
    fun onDmg(e: EntityDamageByEntityEvent) {
        if (e.damager !is Player) {
            return
        }
        val attacker = e.damager as Player
        var dmg = (e.damage * config.getDouble("reflection_percentage") / 100)
        
        val list = attacker.inventory.storageContents
        var targetOrb: ItemStack? = null
        do {
            for (value in list) {
                if (value != null && value.type == Material.ECHO_SHARD && value.persistentDataContainer.has(NamespacedKey(this, "durability"))) {
                    targetOrb = value
                    break
                }
            }
            if (targetOrb == null) {
                break
            } else {
                var meta = targetOrb.itemMeta
                var origin = meta.persistentDataContainer.get(NamespacedKey(this, "durability"), PersistentDataType.INTEGER)
                var result = origin!! - dmg.roundToInt()
                if (result > 0) {
                    meta.persistentDataContainer.set(NamespacedKey(this, "durability"), PersistentDataType.INTEGER, result)
                    meta.lore(mutableListOf<Component>(Component.text("반사 피해를 일정량 방어해줍니다.", NamedTextColor.LIGHT_PURPLE), Component.text("내구도 : $result / ${config.getInt("orb_durability")}", NamedTextColor.YELLOW)))
                    targetOrb.itemMeta = meta
                    if (config.getBoolean("orb_guard_message"))
                        attacker.sendMessage(Component.text("${dmg}의 반사 피해를 방어했습니다! 내구도가 ${result}만큼 남았습니다!", NamedTextColor.RED))
                    dmg = 0.0
                    return
                } else {
                    dmg -= origin
                    attacker.inventory.remove(targetOrb)
                    if (config.getBoolean("orb_broken_message")) {
                        attacker.sendMessage(Component.text("오브 하나가 파괴되었습니다!", NamedTextColor.RED))
                    }
                    targetOrb = null
                    if (config.getBoolean("orb_broken_guard")) {
                        dmg = 0.0
                        return
                    }
                }
            }
        } while (true)
        
        attacker.damage(dmg)
    }
}
