package controller;

import model.GameModel;
import view.GameView;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GameController extends KeyAdapter implements ActionListener {
    private final GameModel model;
    private final GameView view;
    private final Timer timer = new Timer(16, this);
    private boolean jump, down;

    public GameController(GameModel model, GameView view) {
        this.model = model;
        this.view = view;
        view.addKeyListener(this);
    }

    public void start() {
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        model.tick(jump, down);
        view.repaint();
    }

    private static boolean isJumpKey(int key) {
        return key == KeyEvent.VK_UP || key == KeyEvent.VK_W || key == KeyEvent.VK_SPACE;
    }

    private static boolean isDownKey(int key) {
        return key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S || key == KeyEvent.VK_SHIFT;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (isJumpKey(key)) jump = true;
        if (isDownKey(key)) down = true;
        boolean confirm = key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        boolean escape = key == KeyEvent.VK_ESCAPE;

        switch (model.getState()) {
            case MENU -> {
                if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) model.moveMenuSelection(-1);
                if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) model.moveMenuSelection(1);
                if (confirm) {
                    jump = false; // não pular sozinha ao começar
                    switch (model.getMenuIndex()) {
                        case GameModel.MENU_STORY -> model.startIntro();
                        case GameModel.MENU_FREE -> model.start(GameModel.Mode.FREE);
                        case GameModel.MENU_OPTIONS -> model.openOptions();
                        default -> System.exit(0);
                    }
                }
                if (escape) System.exit(0);
            }
            case OPTIONS -> {
                if (confirm || escape) model.closeOptions();
            }
            case INTRO -> {
                if (confirm) model.advanceIntro();
                if (escape) model.skipIntro();
                jump = false; // não pular sozinha quando a corrida começar
            }
            case PLAYING -> {
                if (escape || key == KeyEvent.VK_P) model.pause();
            }
            case PAUSED -> {
                if (escape || key == KeyEvent.VK_P) model.resume();
                if (key == KeyEvent.VK_Q) model.backToMenu();
            }
            case GAME_OVER -> {
                if (confirm) {
                    jump = false;
                    model.restart();
                }
                if (escape) model.backToMenu();
            }
            case STORY_END -> {
                if ((confirm || escape) && model.getEndTick() > 60) model.backToMenu();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (isJumpKey(e.getKeyCode())) jump = false;
        if (isDownKey(e.getKeyCode())) down = false;
    }
}
