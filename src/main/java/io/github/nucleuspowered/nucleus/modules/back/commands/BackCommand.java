/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.commands;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Maps;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimManager;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.modules.back.config.BackConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.back.listeners.BackListeners;
import io.github.nucleuspowered.nucleus.modules.back.services.BackHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;

import java.util.Map;
import java.util.Optional;

@Permissions
@RegisterCommand({"back", "return"})
@EssentialsEquivalent({"back", "return"})
@NonnullByDefault
public class BackCommand extends AbstractCommand<Player> implements Reloadable {

    private final BackHandler handler = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(BackHandler.class);
    private boolean sameDimensionCheck = false;
    private final String EXEMPT_SAMEDIM_PERMISSION = this.permissions.getPermissionWithSuffix(BackListeners.SAME_DIMENSION);
    private final String EXEMPT_ADMINCLAIM_PERMISSION = this.permissions.getPermissionWithSuffix(BackListeners.ADMIN_CLAIM);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags()
                    .permissionFlag(this.permissions.getPermissionWithSuffix("exempt.bordercheck"),"b", "-border")
                    .flag("f", "-force")
                    .buildWith(GenericArguments.none())
        };
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = Maps.newHashMap();
        m.put(BackListeners.ON_DEATH, PermissionInformation.getWithTranslation("permission.back.ondeath", SuggestedLevel.USER));
        m.put(BackListeners.ON_TELEPORT, PermissionInformation.getWithTranslation("permission.back.onteleport", SuggestedLevel.USER));
        m.put(BackListeners.ON_PORTAL, PermissionInformation.getWithTranslation("permission.back.onportal", SuggestedLevel.USER));
        m.put(BackListeners.SAME_DIMENSION, PermissionInformation.getWithTranslation("permission.back.exempt.samedimension", SuggestedLevel.MOD));
        m.put(BackListeners.ADMIN_CLAIM, PermissionInformation.getWithTranslation("permission.back.exempt.adminclaim", SuggestedLevel.MOD));
        m.put("exempt.bordercheck", PermissionInformation.getWithTranslation("permission.tppos.border", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        Optional<Transform<World>> ol = this.handler.getLastLocation(src);
        if (!ol.isPresent()) {
            throw ReturnMessageException.fromKey(src, "command.back.noloc");
        }

        Transform<World> loc = ol.get();
        if (this.sameDimensionCheck && src.getWorld().getUniqueId() != loc.getExtent().getUniqueId()) {
            if (!hasPermission(src, EXEMPT_SAMEDIM_PERMISSION)) {
                throw ReturnMessageException.fromKey(src, "command.back.sameworld");
            }
        }
        if (!hasPermission(src, EXEMPT_ADMINCLAIM_PERMISSION)) {
            ClaimManager claimManager = GriefDefender.getCore().getClaimManager(ol.get().getExtent().getUniqueId());
            Vector3d vec = loc.getPosition();
            Claim claim = claimManager.getClaimAt(new Vector3i(vec.getX(), vec.getY(), vec.getZ()));
            if (claim.isAdminClaim()) {
                throw ReturnMessageException.fromKey(src, "command.back.adminclaim");
            }
        }

        NucleusTeleportHandler.TeleportResult result =
                Nucleus.getNucleus().getTeleportHandler()
                    .teleportPlayer(src, loc, !args.hasAny("f"), !args.hasAny("b"));
        if (result.isSuccess()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.back.success"));
            return CommandResult.success();
        } else if (result == NucleusTeleportHandler.TeleportResult.FAILED_NO_LOCATION) {
            throw ReturnMessageException.fromKey("command.back.nosafe");
        }

        throw ReturnMessageException.fromKey("command.back.cancelled");
    }

    @Override
    public void onReload() throws Exception {
        this.sameDimensionCheck = getServiceUnchecked(BackConfigAdapter.class).getNodeOrDefault().isOnlySameDimension();
    }
}
