import streamlit as st

# Configuració de la pàgina
st.set_page_config(
    page_title="Rescat a l'Abisme - Simulació IA", page_icon="🏔️", layout="wide"
)

# Inicialització de l'estat de la partida
if "etapa" not in st.session_state:
    st.session_state.etapa = "inici"
if "rol" not in st.session_state:
    st.session_state.rol = None
if "oxigen" not in st.session_state:
    st.session_state.oxigen = 100
if "reputacio" not in st.session_state:
    st.session_state.reputacio = 80
if "historial" not in st.session_state:
    st.session_state.historial = []

# --- TÍTOL I CONTEXT ---
st.title("🏔️ Rescat a l'Abisme: Simulació d'Alta Muntanya")
st.markdown(
    "*Un simulador de presa de decisions sota pressió amb IA agèntica.*"
)

# --- FASE 1: SELECCIÓ DE ROL ---
if st.session_state.etapa == "inici":
    st.header("1. Selecciona el teu Rol")
    col1, col2, col3 = st.columns(3)

    with col1:
        st.subheader("🧭 Guia de Muntanya")
        st.write(
            "**Objectiu:** Prioritzar la supervivència de l'equip i la seguretat."
        )
        st.write("*Info privada:* El temps empitjorarà ràpidament.")
        if st.button("Triar Guia"):
            st.session_state.rol = "Guia"
            st.session_state.etapa = "ronda_1"
            st.rerun()

    with col2:
        st.subheader("💻 Científic de Camp")
        st.write(
            "**Objectiu:** Assegurar les mostres de recerca a la cimera."
        )
        st.write("*Info privada:* Les mostres perden valor si es congelen.")
        if st.button("Triar Científic"):
            st.session_state.rol = "Científic"
            st.session_state.etapa = "ronda_1"
            st.rerun()

    with col3:
        st.subheader("📹 Videògraf / Comunicador")
        st.write(
            "**Objectiu:** Mantenir la reputació pública i de l'expedició."
        )
        st.write("*Info privada:* Hi ha pressió dels patrocinadors exteriors.")
        if st.button("Triar Videògraf"):
            st.session_state.rol = "Videògraf"
            st.session_state.etapa = "ronda_1"
            st.rerun()

# --- BARRA LATERAL D'ESTAT ---
elif st.session_state.etapa in [
    "ronda_1",
    "ronda_2",
    "ronda_3",
    "ronda_4",
    "final",
]:
    with st.sidebar:
        st.header("📊 Estat de la Missió")
        st.info(f"**Rol actual:** {st.session_state.rol}")
        st.metric("Oxigen / Recursos", f"{st.session_state.oxigen}%")
        st.metric("Reputació / Moral", f"{st.session_state.reputacio}%")

        if st.button("🔄 Reiniciar Partida"):
            st.session_state.etapa = "inici"
            st.session_state.rol = None
            st.session_state.oxigen = 100
            st.session_state.reputacio = 80
            st.session_state.historial = []
            st.rerun()

    # --- RONDA 1 ---
    if st.session_state.etapa == "ronda_1":
        st.header("Ronda 1: Canvi sobtat del temps")
        st.write(
            "El baròmetre cau en picat. S'aproxima una tempesta de neu lleugera just abans d'arribar al Camp Base 1."
        )

        accio = st.radio(
            "Quina decisió preneu?",
            [
                "Continuar avançant ràpidament per guanyar temps.",
                "Aturar-se i muntar refugi immediatament.",
                "Enviar un avançat per avaluar la ruta mentre s'espera.",
            ],
        )

        if st.button("Confirmar Decisió (Ronda 1)"):
            if "Continuar" in accio:
                st.session_state.oxigen -= 15
                st.session_state.reputacio += 5
            elif "Aturar-se" in accio:
                st.session_state.oxigen -= 5
                st.session_state.reputacio -= 10
            else:
                st.session_state.oxigen -= 10
                st.session_state.reputacio += 0

            st.session_state.etapa = "ronda_2"
            st.rerun()

    # --- RONDA 2 ---
    elif st.session_state.etapa == "ronda_2":
        st.header("Ronda 2: Fallada d'equipament")
        st.write(
            "Un dels tancs auxiliars d'oxigen presenta fuites. Si no es repara o s'abandona, suposarà un llast constant."
        )

        accio = st.radio(
            "Quina acció executeu?",
            [
                "Reparar-lo d'urgència gastant temps i energia extra.",
                "Abandonar el tanc i repartir el risc entre l'equip.",
                "Reduir el ritme general de marxa per estalviar.",
            ],
        )

        if st.button("Confirmar Decisió (Ronda 2)"):
            if "Reparar-lo" in accio:
                st.session_state.oxigen -= 20
                st.session_state.reputacio += 10
            elif "Abandonar" in accio:
                st.session_state.oxigen -= 10
                st.session_state.reputacio -= 15
            else:
                st.session_state.oxigen -= 5
                st.session_state.reputacio -= 5

            st.session_state.etapa = "ronda_3"
            st.rerun()

    # --- RONDA 3 ---
    elif st.session_state.etapa == "ronda_3":
        st.header("Ronda 3: Discrepància a l'equip")
        st.write(
            "Sorgeixen dubtes greus sobre si continuar cap al cim o prioritzar la retirada segura a causa del cansament acumulat."
        )

        accio = st.radio(
            "Com gestioneu el conflicte?",
            [
                "Imposar criteri estricte de la direcció de l'expedició.",
                "Obrir debat col·lectiu i votar democràticament.",
                "Cedir a les pressions externes i continuar a la desesperada.",
            ],
        )

        if st.button("Confirmar Decisió (Ronda 3)"):
            if "Imposar" in accio:
                st.session_state.oxigen -= 10
                st.session_state.reputacio -= 5
            elif "debat" in accio:
                st.session_state.oxigen -= 15
                st.session_state.reputacio += 10
            else:
                st.session_state.oxigen -= 30
                st.session_state.reputacio -= 20

            st.session_state.etapa = "ronda_4"
            st.rerun()

    # --- RONDA 4 ---
    elif st.session_state.etapa == "ronda_4":
        st.header("Ronda 4: L'última cruïlla de la cimera")
        st.write(
            "Esteu a un pas de l'objectiu final, però la finestra de bon temps s'està tancant completament."
        )

        accio = st.radio(
            "Escull l'última jugada:",
            [
                "Assumir el risc màxim i intentar coronar el cim.",
                "Iniciar la retirada tàctica amb els assoliments parcials.",
            ],
        )

        if st.button("Finalitzar Partida"):
            if "Assumir" in accio:
                st.session_state.oxigen -= 20
                st.session_state.reputacio += 20
            else:
                st.session_state.oxigen += 0
                st.session_state.reputacio += 5

            st.session_state.etapa = "final"
            st.rerun()

    # --- PANTALLA FINAL ---
    elif st.session_state.etapa == "final":
        st.header("🏁 Resultat Final de la Simulació")

        if (
            st.session_state.oxigen > 0
            and st.session_state.reputacio >= 50
        ):
            st.success(
                "🎉 **Missió Èpica Complerta!** L'equip ha retornat amb èxit i reconeixement."
            )
        else:
            st.error(
                "⚠️ **Missió Compromesa.** Les decisions preses han portat l'expedició al límit de la fallida."
            )

        st.write(f"**Oxigen final restant:** {st.session_state.oxigen}%")
        st.write(f"**Reputació final:** {st.session_state.reputacio}%")

        st.markdown("### 🤖 Avaluació de l'Agent IA (Director de Partida)")
        st.info(
            f"L'agent analitza que amb el rol de **{st.session_state.rol}**, "
            "heu demostrat una gestió de recursos orientada al risc sota pressió, "
            "equilibrant els objectius individuals amb la supervivència col·lectiva."
        )

        if st.button("Jugar una altra vegada"):
            st.session_state.etapa = "inici"
            st.session_state.rol = None
            st.session_state.oxigen = 100
            st.session_state.reputacio = 80
            st.rerun()