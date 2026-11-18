package model;

import java.util.List;

// ============================================================
//  FALAS DA CUTSCENE (antes do modo história)
//  Edite os textos à vontade. Cada fala escolhe um "rosto":
//    SORRINDO, PISCANDO, RINDO, TIMIDO, APAIXONADO
// ============================================================
public final class Falas {
    private Falas() { }

    public enum Rosto { SORRINDO, PISCANDO, RINDO, TIMIDO, APAIXONADO }

    public record Fala(Rosto rosto, String texto) { }

    public static final List<Fala> INTRO = List.of(
        new Fala(Rosto.SORRINDO, "Oi, meu amor! Como você está, animada?"),
        new Fala(Rosto.RINDO, "Como você sabe, esse período na faculdade precisei fazer um jogo e então eu pensei comigo mesmo"),
        new Fala(Rosto.SORRINDO, "Caramba, eu e meu amor vamos fazer 2 anos de namoro. Ela sente muita falta da minha criatividade e eu tenho muita dificuldade de pensar nessas coisas, vou aproveitar essa oportunidade e fazer um joguinho para ela."),
        new Fala(Rosto.RINDO, "E bom, um tempinho atrás te entreguei um desenho nosso e agora fiz esse jogo especialmente para você!"),
        new Fala(Rosto.TIMIDO, "Eu não tinha ideia do que eu te daria de presente de namoro, então pensei nesse joguinho simples não somente como forma de te presentear e comemorar nossos 2 anos de namoro."),
        new Fala(Rosto.RINDO, "Mas para te lembrar que eu to me esforçando muito para melhor e demonstrar um pouquinho do meu amor por você"),
        new Fala(Rosto.APAIXONADO, "Inclusive, eu te amo mais, sabia?"),
        new Fala(Rosto.PISCANDO, "Mas bom, a sua missão nesse jogo é simples: correr até mim!"),
        new Fala(Rosto.SORRINDO, "Pule os pinheiros, deslize por baixo dos pássaros e pegue os corações pelo caminho."),
        new Fala(Rosto.PISCANDO, "Ah, e você só tem 3 vidas... Então toma cuidado, tá?"),
        new Fala(Rosto.APAIXONADO, "Estou te esperando no final.")
    );
}
