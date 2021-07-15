package com.joemasone.mariobros.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joemasone.mariobros.MarioBros;
import com.joemasone.mariobros.Scenes.Hud;
import com.joemasone.mariobros.Sprites.Enemy.Enemy;
import com.joemasone.mariobros.Sprites.Items.Item;
import com.joemasone.mariobros.Sprites.Items.ItemDef;
import com.joemasone.mariobros.Sprites.Items.Mushroom;
import com.joemasone.mariobros.Sprites.Mario;
import com.joemasone.mariobros.Tools.B2WorldCreator;
import com.joemasone.mariobros.Tools.WorldContactListener;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class PlayScreen implements Screen {
    //Sprites
    private Mario player;
    private boolean inAir;

    private TextureAtlas atlas;

    private MarioBros game;
    private OrthographicCamera gamecam;
    private Viewport gamePort;
    private Hud hud;

    //Tiled map variables
    private TmxMapLoader mapLoader;
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    //Box2d variables
    private World world;
    private Box2DDebugRenderer b2dr;
    private B2WorldCreator creator;

    private Music music;

    private Array<Item> items;
    private LinkedBlockingQueue<ItemDef> itemsToSpawn;

    //Constructor
    public PlayScreen(MarioBros game) {

        atlas = new TextureAtlas("Mario_and_Enemies.txt");

        this.game = game;

        //create cam used to follow mario through camworld
        gamecam = new OrthographicCamera();


        // Viewport to maintain virtual aspect ratio
        gamePort = new FitViewport(MarioBros.V_WIDTH / MarioBros.PPM, MarioBros.V_HEIGHT / MarioBros.PPM, gamecam);

        //Creating game HUD score/timer/level
        hud = new Hud(game.batch);//creating hud

        //Load map and setup map renderer
        mapLoader = new TmxMapLoader();
        map = mapLoader.load("level1.tmx");
        renderer = new OrthogonalTiledMapRenderer(map, 1 / MarioBros.PPM);

        //Initially set gamecam to be centered correctly
        gamecam.position.set(gamePort.getWorldWidth() / 2, gamePort.getWorldHeight() / 2, 0);

        //Create Box2D world, no gravity in x, -10 in y, and allow bodies to sleep
        world = new World(new Vector2(0, -10), true);

        //create mario in game world
        player = new Mario(this);

        //allows for debug lines in box2D world
        b2dr = new Box2DDebugRenderer();

        creator = new B2WorldCreator(this);

        world.setContactListener(new WorldContactListener());

        //playing music file
        music = MarioBros.manager.get("audio/music/mario_music.ogg", Music.class);
        music.setLooping(true);
        music.play();

        items = new Array<Item>();
        itemsToSpawn = new LinkedBlockingQueue<ItemDef>();


    }//end constructor

    public void spawnItem(ItemDef idef){
        itemsToSpawn.add(idef);
    }

    public void handleSpawningItems(){
        if(!itemsToSpawn.isEmpty()){
            ItemDef idef = itemsToSpawn.poll();
            if(idef.type == Mushroom.class){
                items.add(new Mushroom(this, idef.position.x, idef.position.y));
            }
        }
    }

    public TextureAtlas getAtlas(){
        return atlas;
    }

    @Override
    public void show(){

    }

    public void handleInput(float dt){
        //control player using linear impulse if mario is not dead
        if(player.currentState != Mario.State.DEAD) {
            if (player.currentState == Mario.State.JUMPING || player.currentState == Mario.State.FALLING) {
                inAir = true;
            }else
                inAir = false;
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP) && !inAir)
                player.b2body.applyLinearImpulse(new Vector2(0, 4f), player.b2body.getWorldCenter(), true);

            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && player.b2body.getLinearVelocity().x <= 2)
                player.b2body.applyLinearImpulse(new Vector2(0.1f, 0), player.b2body.getWorldCenter(), true);

            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && player.b2body.getLinearVelocity().x >= -2)
                player.b2body.applyLinearImpulse(new Vector2(-0.1f, 0), player.b2body.getWorldCenter(), true);
        }
    }

    public void update(float dt){
        //handle user input first
        handleInput(dt);
        handleSpawningItems();

        //takes 1 step in physics simulation(60 times/second)
        world.step(1/60f, 6, 2);

        player.update(dt);
        for(Enemy enemy : creator.getEnemies()){
            enemy.update(dt);
            if(enemy.getX() < player.getX() + 224 / MarioBros.PPM){
                enemy.b2body.setActive(true);
            }
        }

        for(Item item : items){
            item.update(dt);
        }

        hud.update(dt);

        //attach gamecam to players.x coordinate
        if(player.currentState != Mario.State.DEAD){
            gamecam.position.x = player.b2body.getPosition().x;
        }

        //update gamecam with correct coordinates after changes
        gamecam.update();

        //tell renderer to draw only what camera can see
        renderer.setView(gamecam);
    }

    @Override
    public void render(float delta) {
        //separate update logic from render
        update(delta);

        //Clear game screen with black
        Gdx.gl.glClearColor(0,0,0,1);//color of background
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //render game map
        renderer.render();

        //render Box2DDebugLines
        b2dr.render(world, gamecam.combined);

        game.batch.setProjectionMatrix(gamecam.combined);
        game.batch.begin();
        player.draw(game.batch);
        for(Enemy enemy : creator.getEnemies()){
            enemy.draw(game.batch);
        }
        for(Item item : items){
            item.draw(game.batch);
        }
        game.batch.end();

        //Set batch to draw what the HUD camera sees
        game.batch.setProjectionMatrix(hud.stage.getCamera().combined);
        hud.stage.draw();

        if(gameOver()){
            game.setScreen(new GameOverScreen(game));
            dispose();
        }
    }

    public boolean gameOver(){
        if(player.currentState == Mario.State.DEAD && player.getStateTimer() > 3){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void resize(int width, int height) {
        gamePort.update(width, height);
    }

    public TiledMap getMap(){
        return map;
    }

    public World getWorld(){
        return world;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        map.dispose();
        renderer.dispose();
        world.dispose();
        b2dr.dispose();
        hud.dispose();

    }
}
