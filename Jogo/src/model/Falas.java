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
        new Fala(Rosto.RINDO, "Como você sabe, precisei fazer um jogo na faculdade e então eu pensei: poxa, essa é uma grande oportunidade, vou me esforçar já que meu amorzinho sente falta da minha criatividade."),
        new Fala(Rosto.SORRINDO, "E bom, um tempinho atrás te entreguei um desenho e agora fiz esse jogo especialmente para você!"),
        new Fala(Rosto.TIMIDO, "Eu não tinha ideia do que eu te daria de presente de namoro, então pensei nesse joguinho simples como forma de te presentear e claro para comemorar nossos 2 ANOS DE NAMORO!"),
        new Fala(Rosto.RINDO, "Eu e nosso amigo Claudinho nos empenhamos bastante para terminar ele. Fui dando as ideias, ele me ajudando a fazer e eu ajustando o que precisava."),
        new Fala(Rosto.APAIXONADO, "Eu te amo mais, sabia?"),
        new Fala(Rosto.PISCANDO, "Mas bom, a sua missão é simples: correr até mim!"),
        new Fala(Rosto.SORRINDO, "Pule os pinheiros, deslize por baixo dos pássaros e pegue os corações pelo caminho."),
        new Fala(Rosto.PISCANDO, "Ah, e você só tem 3 vidas... Então toma cuidado, tá?"),
        new Fala(Rosto.APAIXONADO, "Te espero no final.")
    );
}
