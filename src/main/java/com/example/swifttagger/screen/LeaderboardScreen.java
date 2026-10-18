package com.example.swifttagger.screen;

import com.example.swifttagger.SwiftTaggerClient;
import com.example.swifttagger.TierChatDisplay;
import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.skin.PlayerHeadCache;
import com.example.swifttagger.text.Messages;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.*;

public class LeaderboardScreen extends Screen {
    private final Screen parent;
    private List<PlayerProfile> players = List.of();
    private boolean loading = true;
    private boolean loadError = false;
    private int scroll = 0;
    private float hoverAlpha = 0f;

    public LeaderboardScreen(Screen parent) {
        super(Text.literal(Messages.get("leaderboard.title")));
        this.parent = parent;
    }

    @Override protected void init() { load(); }

    private void load() {
        loading = true;
        loadError = false;
        SwiftTaggerClient.API_CLIENT.fetchAllProfiles()
                .orTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .whenComplete((result, error) -> {
                    if (client == null) return;
                    client.execute(() -> {
                        loadError = error != null;
                        players = error == null && result != null ? result : List.of();
                        loading = false;
                        for (PlayerProfile p : players) if (p.name != null) PlayerHeadCache.request(p.name);
                    });
                });
    }

    @Override public void render(DrawContext ctx,int mouseX,int mouseY,float delta) {
        super.render(ctx,mouseX,mouseY,delta);
        int w=Math.min(420,width-40), x=(width-w)/2, top=24, bottom=height-28;
        int headerH = 24;

        ctx.fill(x,top,x+w,bottom,0xE0101010);
        ctx.fill(x,top,x+w,top+headerH,0x50000000);
        ctx.drawText(textRenderer,Text.literal(Messages.get("leaderboard.title")),x+12,top+9,0xFFFFFFFF,true);
        if (loading) { ctx.drawText(textRenderer,Text.literal(Messages.get("leaderboard.loading")),x+12,top+headerH+8,0xFFAAAAAA,true); return; }
        if (players.isEmpty()) { ctx.drawText(textRenderer,Text.literal(loadError ? Messages.get("leaderboard.error") : Messages.get("leaderboard.empty")),x+12,top+headerH+8,0xFFAAAAAA,true); return; }
        int rowH=22, start=top+headerH+8, visible=Math.max(1,(bottom-start-8)/rowH);
        scroll=Math.max(0,Math.min(scroll,Math.max(0,players.size()-visible)));

        int hoveredRow = -1;
        if (mouseX>=x && mouseX<=x+w && mouseY>=start && mouseY<start+visible*rowH) {
            int hi = (int)((mouseY-start)/rowH);
            if (hi>=0 && scroll+hi<players.size()) hoveredRow = hi;
        }

        float target = hoveredRow>=0 ? 1f : 0f;
        hoverAlpha += (target-hoverAlpha) * 0.35f;
        if (Math.abs(target-hoverAlpha) < 0.01f) hoverAlpha = target;

        for(int i=0;i<visible && scroll+i<players.size();i++){
            PlayerProfile p=players.get(scroll+i); int rank=scroll+i+1, y=start+i*rowH; int c=PlayerCardScreen.rankingColor(rank);
            if (i==hoveredRow && hoverAlpha>0.01f) {
                int alpha = (int)(hoverAlpha*40);
                ctx.fill(x+1,y-3,x+w-1,y+rowH-5,(alpha<<24)|0xFFFFFF);
            }
            PlayerCardScreen.drawCrafatarHead(ctx, p.name, x+10, y, 16);
            ctx.drawText(textRenderer,Text.literal("#"+rank),x+32,y+4,c,true);
            ctx.drawText(textRenderer,Text.literal(p.name),x+76,y+4,0xFFFFFFFF,true);
            if (p.sanction != null) {

                int sanctionColor = p.sanction.resolveColorRgb(com.example.swifttagger.config.ConfigManager.get());
                Text sanction = Text.literal(p.sanction.label).styled(style -> style.withColor(sanctionColor).withBold(true));
                ctx.drawText(textRenderer, sanction, x+w-130, y+4, 0xFFAAAAAA, true);
            } else {
                String pointsLabel = Messages.get("profile.points_label");
                Text points = Text.literal(pointsLabel).append(Text.literal(String.valueOf(p.getPointsOrCalculated())).styled(style -> style.withColor(c)));
                ctx.drawText(textRenderer, points, x+w-130, y+4, 0xFFAAAAAA, true);
            }
        }

        String total = Messages.get("leaderboard.total", "count", players.size());
        ctx.drawText(textRenderer,Text.literal(total),x+12,bottom-16,0xFF777777,false);
        String hint = Messages.get("leaderboard.click_hint");
        int hintX = x + w - 12 - textRenderer.getWidth(hint);
        ctx.drawText(textRenderer,Text.literal(hint),hintX,bottom-16,0xFF777777,false);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled){
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        int w=Math.min(420,width-40), x=(width-w)/2, top=24, start=top+32, rowH=22;
        int idx=(int)((mouseY-start)/rowH)+scroll;
        if(button==0 && mouseX>=x && mouseX<=x+w && idx>=0 && idx<players.size() && mouseY>=start){
            TierChatDisplay.show(players.get(idx).name); return true;
        }
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){ scroll += vertical<0?1:-1; return true; }
    @Override public boolean shouldPause(){return false;}
}
