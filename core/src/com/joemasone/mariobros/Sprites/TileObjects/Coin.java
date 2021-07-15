package com.joemasone.mariobros.Sprites.TileObjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.joemasone.mariobros.MarioBros;
import com.joemasone.mariobros.Scenes.Hud;
import com.joemasone.mariobros.Screens.PlayScreen;
import com.joemasone.mariobros.Sprites.Items.ItemDef;
import com.joemasone.mariobros.Sprites.Items.Mushroom;
import com.joemasone.mariobros.Sprites.Mario;
import com.joemasone.mariobros.Sprites.TileObjects.InteractiveTileObject;

public class Coin extends InteractiveTileObject {

    private static TiledMapTileSet tileSet;
    private final int BLANK_COIN = 28;

    public Coin(PlayScreen screen, MapObject object){
        super(screen, object);
        tileSet = map.getTileSets().getTileSet("tileset_gutter");
        fixture.setUserData(this);
        setCategoryFilter(MarioBros.COIN_BIT);
    }

    @Override
    public void onHeadHit(Mario mario) {
        Gdx.app.log("Coin", "Collision");
        if (getCell().getTile().getId() == BLANK_COIN){
            MarioBros.manager.get("audio/sounds/bump.wav", Sound.class).play();
        }else{
            if(object.getProperties().containsKey("mushroom")) {
                screen.spawnItem(new ItemDef(new Vector2(body.getPosition().x, body.getPosition().y + 16 / MarioBros.PPM),
                        Mushroom.class));
                MarioBros.manager.get("audio/sounds/powerup_spawn.wav", Sound.class).play();
            }else {
                MarioBros.manager.get("audio/sounds/coin.wav", Sound.class).play();
            }
        }

        getCell().setTile(tileSet.getTile(BLANK_COIN));
        Hud.addScore(200);
    }
}
