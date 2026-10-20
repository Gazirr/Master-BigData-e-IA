import random
import streamlit as st

st.set_page_config(
    page_title="El Túnel de los Artefactos - Simulador IA",
    page_icon="🧝",
    layout="wide",
)

# --- INICIALIZACIÓN DEL ESTADO ---
if "etapa" not in st.session_state:
    st.session_state.etapa = "introduccion"
if "modo" not in st.session_state:
    st.session_state.modo = "Solitario"
if "rol_jugador" not in st.session_state:
    st.session_state.rol_jugador = None
if "vida_jugador" not in st.session_state:
    st.session_state.vida_jugador = 100
if "max_vida_jugador" not in st.session_state:
    st.session_state.max_vida_jugador = 100
if "artefactos" not in st.session_state:
    st.session_state.artefactos = []
if "habilidades_desbloqueadas" not in st.session_state:
    st.session_state.habilidades_desbloqueadas = ["Ataque Base"]
if "puerta_actual" not in st.session_state:
    st.session_state.puerta_actual = None
if "ronda_actual" not in st.session_state:
    st.session_state.ronda_actual = 1
if "vida_enemigo" not in st.session_state:
    st.session_state.vida_enemigo = 0
if "max_vida_enemigo" not in st.session_state:
    st.session_state.max_vida_enemigo = 0
if "historial_log" not in st.session_state:
    st.session_state.historial_log = []
if "puertas_completadas" not in st.session_state:
    st.session_state.puertas_completadas = []

# --- ROLES ---
ROLES = {
    "Médico": {
        "vida": 110,
        "fuerza": 12,
        "hab_inicial": "Pulso Regenerativo",
        "desc_hab": "Recupera 25 de vida.",
        "fuerte": "Gran resistencia y capacidad de curación.",
        "debil": "Ataque base moderado.",
    },
    "Físico": {
        "vida": 90,
        "fuerza": 18,
        "hab_inicial": "Análisis de Debilidades",
        "desc_hab": "Realiza 28 de daño ignorando armadura.",
        "fuerte": "Daño táctico alto y detección de puntos débiles.",
        "debil": "Poca vida máxima.",
    },
    "Montañista": {
        "vida": 120,
        "fuerza": 14,
        "hab_inicial": "Paso Ligero",
        "desc_hab": "Ataca con 16 de daño y reduce el próximo impacto recibido.",
        "fuerte": "Excelente movilidad y aguante.",
        "debil": "Ataques de rango bajo.",
    },
    "Profesor": {
        "vida": 100,
        "fuerza": 15,
        "hab_inicial": "Inspiración Táctica",
        "desc_hab": "Sube el moral y realiza 22 de daño.",
        "fuerte": "Gran estratega, impulsa la eficacia de los bots.",
        "debil": "Estadísticas equilibradas sin pico de fuerza.",
    },
    "Atleta": {
        "vida": 105,
        "fuerza": 16,
        "hab_inicial": "Sprint Veloz",
        "desc_hab": "Dos golpes rápidos de 12 de daño cada uno.",
        "fuerte": "Velocidad y ataques múltiples por turno.",
        "debil": "Defensa reducida tras usar ataques potentes.",
    },
    "Soldado": {
        "vida": 130,
        "fuerza": 20,
        "hab_inicial": "Impacto Devastador",
        "desc_hab": "Golpe pesado de 32 de daño.",
        "fuerte": "Alta vida y gran daño de impacto.",
        "debil": "Lento para reaccionar o esquivar.",
    },
}

# --- PUERTAS Y ENEMIGOS ---
PUERTAS = {
    "Puerta 1": {
        "nombre": "Las Cavernas de Cristal",
        "artefacto": "El Reloj de Arena Etéreo",
        "habilidad": "Escudo de Cristal",
        "enemigos": [
            {"nombre": "Murciélago de Cristal", "vida": 30, "dano": 6},
            {"nombre": "Escarabajo de Cuarzo", "vida": 45, "dano": 9},
            {"nombre": "Sombra Neón", "vida": 60, "dano": 12},
            {"nombre": "💎 JEFE: Golem de Cristal", "vida": 110, "dano": 18},
        ],
    },
    "Puerta 2": {
        "nombre": "El Lago Subterráneo",
        "artefacto": "La Lágrima Abisal",
        "habilidad": "Salpicadura Curativa",
        "enemigos": [
            {"nombre": "Sanguijuela de Cueva", "vida": 35, "dano": 7},
            {"nombre": "Lurker de la Niebla", "vida": 50, "dano": 10},
            {"nombre": "Cangrejo Abisal", "vida": 70, "dano": 13},
            {"nombre": "🐍 JEFE: Serpiente de las Profundidades", "vida": 120, "dano": 20},
        ],
    },
    "Puerta 3": {
        "nombre": "Las Ruinas Olvidadas",
        "artefacto": "El Códice de los Eones",
        "habilidad": "Golpe Crítico Arcano",
        "enemigos": [
            {"nombre": "Gárgola de Arcilla", "vida": 40, "dano": 8},
            {"nombre": "Espectro del Templo", "vida": 55, "dano": 11},
            {"nombre": "Autómata Oxidado", "vida": 75, "dano": 14},
            {"nombre": "🗿 JEFE: Guardián de Piedra Ancestral", "vida": 135, "dano": 22},
        ],
    },
    "Puerta 4": {
        "nombre": "La Cámara del Magma",
        "artefacto": "El Núcleo de Magma",
        "habilidad": "Furia de Fuego",
        "enemigos": [
            {"nombre": "Elemental de Ceniza", "vida": 45, "dano": 9},
            {"nombre": "Sierpe Infernilla", "vida": 60, "dano": 12},
            {"nombre": "Bestia Basáltica", "vida": 80, "dano": 15},
            {"nombre": "🔥 JEFE: Salamandra de Fuego", "vida": 150, "dano": 25},
        ],
    },
}

def registrar_log(texto):
    st.session_state.historial_log.append(texto)

# --- CABECERA ---
st.title("🧝 El Túnel de los Artefactos")
st.caption("Simulador Gamificado con IA Agèntica | Basado en dinámicas de decisión sota pressió")

# --- BARRA LATERAL (VIDA DINÁMICA) ---
if st.session_state.etapa in ["hub_puertas", "combate", "puerta_completada", "final"]:
    with st.sidebar:
        st.header("📊 Estado del Jugador")
        st.write(f"**Rol:** {st.session_state.rol_jugador}")
        st.write(f"**Modo:** {st.session_state.modo}")
        
        # Cálculo y renderizado dinámico de la barra de vida
        porcentaje_vida = max(0, min(100, int((st.session_state.vida_jugador / st.session_state.max_vida_jugador) * 100)))
        st.progress(porcentaje_vida / 100.0)
        
        if porcentaje_vida > 50:
            st.write(f"💚 **Vida:** {st.session_state.vida_jugador} / {st.session_state.max_vida_jugador} HP ({porcentaje_vida}%)")
        elif porcentaje_vida > 20:
            st.write(f"💛 **Vida:** {st.session_state.vida_jugador} / {st.session_state.max_vida_jugador} HP ({porcentaje_vida}%) - ¡Precaución!")
        else:
            st.write(f"❤️ **Vida:** {st.session_state.vida_jugador} / {st.session_state.max_vida_jugador} HP ({porcentaje_vida}%) - ¡Peligro!")

        st.subheader("🏆 Artefactos Recuperados")
        if st.session_state.artefactos:
            for art in st.session_state.artefactos:
                st.success(f"✨ {art}")
        else:
            st.write("Ninguno todavía.")

        st.subheader("🎒 Habilidades Equipadas")
        st.caption("Máximo 1 Ataque Base + 3 Especiales")
        for hab in st.session_state.habilidades_desbloqueadas[:4]:
            st.code(f"• {hab}")

        if st.button("🔄 Reiniciar Partida"):
            st.session_state.clear()
            st.rerun()

# --- PANTALLAS ---
if st.session_state.etapa == "introduccion":
    col_elfo, col_info = st.columns([1, 2])
    with col_elfo:
        st.header("🧝 Elfo Eldrin")
        st.info("¡Viajero! Unos monstruos han robado los **4 Artefactos Sagrados** y los han ocultado en 4 puertas del túnel. Recuperálos para salvar el reino.")
    with col_info:
        st.subheader("Configuración de la Partida")
        modo_sel = st.radio("Selecciona el modo de juego:", ["Solitario (5 Bots controlados por la IA)", "Multijugador (Pase y Juega)"])
        if st.button("Aceptar y Seleccionar Rol"):
            st.session_state.modo = modo_sel
            st.session_state.etapa = "seleccion_rol"
            st.rerun()

elif st.session_state.etapa == "seleccion_rol":
    st.header("1. Selecciona tu Rol de Personaje")
    cols = st.columns(3)
    for index, (nombre_rol, datos) in enumerate(ROLES.items()):
        with cols[index % 3]:
            st.subheader(f"🛡️ {nombre_rol}")
            st.write(f"❤️ **Vida:** {datos['vida']} | ⚔️ **Fuerza:** {datos['fuerza']}")
            st.write(f"✨ **Habilidad:** {datos['hab_inicial']}")
            st.write(f"💪 **Punto Fuerte:** {datos['fuerte']}")
            st.write(f"⚠️ **Punto Débil:** {datos['debil']}")

            if st.button(f"Elegir {nombre_rol}", key=f"btn_{nombre_rol}"):
                st.session_state.rol_jugador = nombre_rol
                st.session_state.vida_jugador = datos["vida"]
                st.session_state.max_vida_jugador = datos["vida"]
                st.session_state.habilidades_desbloqueadas = ["Ataque Base", datos["hab_inicial"]]
                st.session_state.etapa = "hub_puertas"
                st.rerun()

elif st.session_state.etapa == "hub_puertas":
    st.header("🚪 El Túnel de las 4 Puertas")
    st.write("Elige a qué puerta deseas entrar con tu equipo:")

    cols_p = st.columns(2)
    for i, (id_puerta, datos_p) in enumerate(PUERTAS.items()):
        with cols_p[i % 2]:
            st.subheader(f"{id_puerta}: {datos_p['nombre']}")
            st.write(f"🔮 **Artefacto:** {datos_p['artefacto']}")
            st.write(f"⭐ **Habilidad a ganar:** {datos_p['habilidad']}")

            if id_puerta in st.session_state.puertas_completadas:
                st.success("✅ Puerta Completada")
            else:
                if st.button(f"Entrar a {id_puerta}", key=f"entrar_{id_puerta}"):
                    st.session_state.puerta_actual = id_puerta
                    st.session_state.ronda_actual = 1
                    enemigo_info = datos_p["enemigos"][0]
                    st.session_state.vida_enemigo = enemigo_info["vida"]
                    st.session_state.max_vida_enemigo = enemigo_info["vida"]
                    st.session_state.historial_log = []
                    registrar_log(f"Has entrado a {datos_p['nombre']}. Te enfrentas a la Ronda 1!")
                    st.session_state.etapa = "combate"
                    st.rerun()

elif st.session_state.etapa == "combate":
    puerta_info = PUERTAS[st.session_state.puerta_actual]
    ronda_idx = st.session_state.ronda_actual - 1
    enemigo_info = puerta_info["enemigos"][ronda_idx]

    st.header(f"🚪 {puerta_info['nombre']} - Ronda {st.session_state.ronda_actual} / 4")
    st.subheader(f"👾 Enemigo: {enemigo_info['nombre']}")

    col_e1, col_e2 = st.columns(2)
    with col_e1:
        st.metric("❤️ Vida del Enemigo", f"{st.session_state.vida_enemigo} HP")
        st.progress(max(0, st.session_state.vida_enemigo) / st.session_state.max_vida_enemigo)
    with col_e2:
        st.metric("⚔️ Daño Enemigo", f"{enemigo_info['dano']} ATK")

    st.write("---")
    st.subheader("🎯 Selecciona tu Acción:")
    hab_seleccionada = st.selectbox("Acción / Habilidad:", st.session_state.habilidades_desbloqueadas[:4])

    if st.button("Ejecutar Turno de Combate"):
        fuerza_base = ROLES[st.session_state.rol_jugador]["fuerza"]
        dano_jugador = fuerza_base

        if hab_seleccionada == "Ataque Base":
            dano_jugador = fuerza_base + random.randint(1, 5)
            log_jugador = f"Usaste Ataque Base e infligiste {dano_jugador} de daño."
        elif hab_seleccionada == "Pulso Regenerativo":
            curacion = 25
            st.session_state.vida_jugador = min(st.session_state.max_vida_jugador, st.session_state.vida_jugador + curacion)
            dano_jugador = 5
            log_jugador = f"Usaste Pulso Regenerativo (+{curacion} HP) y realizaste 5 de daño."
        elif hab_seleccionada == "Análisis de Debilidades":
            dano_jugador = 28
            log_jugador = f"Usaste Análisis de Debilidades e infligiste {dano_jugador} de daño directo."
        elif hab_seleccionada == "Impacto Devastador":
            dano_jugador = 32
            log_jugador = f"Usaste Impacto Devastador asestando un golpe masivo de {dano_jugador} de daño!"
        else:
            dano_jugador = fuerza_base + 15
            log_jugador = f"Usaste {hab_seleccionada} e infligiste {dano_jugador} de daño especial."

        dano_bots = 0
        if st.session_state.modo.startswith("Solitario"):
            dano_bots = random.randint(8, 16)
            log_bots = f"🤖 La IA (tus 5 compañeros bots) atacó coordinadamente causando {dano_bots} de daño extra."
        else:
            log_bots = "👥 Tus compañeros de equipo apoyaron tu ataque."

        total_dano = dano_jugador + dano_bots
        vida_enemigo_antes = st.session_state.vida_enemigo
        st.session_state.vida_enemigo = max(0, st.session_state.vida_enemigo - total_dano)
        
        registrar_log(log_jugador)
        registrar_log(log_bots)
        registrar_log(f"💥 El enemigo tenía {vida_enemigo_antes} HP, recibió {total_dano} de daño total y ahora le quedan **{st.session_state.vida_enemigo} HP**.")

        if st.session_state.vida_enemigo > 0:
            dano_recibido = enemigo_info["dano"] + random.randint(-2, 3)
            st.session_state.vida_jugador -= dano_recibido
            registrar_log(f"⚠️ {enemigo_info['nombre']} contraatacó infligiéndote {dano_recibido} de daño. (Tu vida actual: {st.session_state.vida_jugador} HP)")

        if st.session_state.vida_jugador <= 0:
            st.session_state.etapa = "final"
            st.rerun()

        if st.session_state.vida_enemigo <= 0:
            if st.session_state.ronda_actual < 4:
                cura_esbirro = 10
                st.session_state.vida_jugador = min(st.session_state.max_vida_jugador, st.session_state.vida_jugador + cura_esbirro)
                st.session_state.ronda_actual += 1
                sig_enemigo = puerta_info["enemigos"][st.session_state.ronda_actual - 1]
                st.session_state.vida_enemigo = sig_enemigo["vida"]
                st.session_state.max_vida_enemigo = sig_enemigo["vida"]
                registrar_log(f"🎉 ¡Derrotaste al enemigo! Recuperas +{cura_esbirro} HP (Tu vida: {st.session_state.vida_jugador} HP). Avanzas a la Ronda {st.session_state.ronda_actual} contra {sig_enemigo['nombre']}.")
            else:
                cura_jefe = 20
                st.session_state.vida_jugador = min(st.session_state.max_vida_jugador, st.session_state.vida_jugador + cura_jefe)
                st.session_state.puertas_completadas.append(st.session_state.puerta_actual)
                st.session_state.artefactos.append(puerta_info["artefacto"])
                if puerta_info["habilidad"] not in st.session_state.habilidades_desbloqueadas:
                    st.session_state.habilidades_desbloqueadas.append(puerta_info["habilidad"])
                registrar_log(f"🏆 ¡Jefe derrotado! Recuperas +{cura_jefe} HP (Tu vida: {st.session_state.vida_jugador} HP).")
                st.session_state.etapa = "puerta_completada"
            st.rerun()

    st.subheader("📜 Registro de Combate")
    for log in reversed(st.session_state.historial_log[-6:]):
        st.write(log)

elif st.session_state.etapa == "puerta_completada":
    puerta_info = PUERTAS[st.session_state.puerta_actual]
    st.session_state.vida_jugador = st.session_state.max_vida_jugador

    st.balloons()
    st.header(f"🏆 ¡Has completado {puerta_info['nombre']}!")
    st.success(f"✨ Has conseguido el artefacto: **{puerta_info['artefacto']}**")
    st.info(f"⭐ Nueva habilidad desbloqueada: **{puerta_info['habilidad']}**")
    st.success("❤️ **¡Vida restaurada al 100% por cambiar de puerta!**")

    st.subheader("🤔 Decisión Estratégica:")
    col_d1, col_d2 = st.columns(2)
    with col_d1:
        if st.button("🚪 Volver al Hub y Elegir Otra Puerta"):
            st.session_state.etapa = "hub_puertas"
            st.rerun()

    with col_d2:
        if len(st.session_state.puertas_completadas) == 4:
            if st.button("👑 Reclamar la Victoria Final"):
                st.session_state.etapa = "final"
                st.rerun()
        else:
            if st.button("🏁 Finalizar Expedición Ahora"):
                st.session_state.etapa = "final"
                st.rerun()

elif st.session_state.etapa == "final":
    st.header("🏁 Resultado Final de la Simulación")

    if st.session_state.vida_jugador > 0 and len(st.session_state.puertas_completadas) == 4:
        st.balloons()
        st.success("🎉 **¡VICTORIA ABSOLUTA!** Has recuperado los 4 Artefactos Sagrados y salvado el reino del Elfo Eldrin.")
    elif st.session_state.vida_jugador > 0:
        st.warning(f"⚔️ **Misión Parcial.** Lograste escapar con {len(st.session_state.artefactos)} de 4 artefactos recuperados.")
    else:
        st.error("☠️ **Derrota.** Tu equipo cayó en combate dentro del túnel antes de completar la misión.")

    st.markdown("### 🤖 Evaluación Agèntica de la IA (Game Master)")
    analisis_ia = (
        f"El jugador eligió el rol **{st.session_state.rol_jugador}** en modo **{st.session_state.modo}**. "
        f"Completó **{len(st.session_state.puertas_completadas)} de 4 puertas** y acumuló "
        f"**{len(st.session_state.habilidades_desbloqueadas)} habilidades**. "
        f"La gestión de combate demostró una estrategia adaptativa bajo presión, equilibrando "
        f"el ataque de su personaje con el soporte agéntico de los bots en cada ronda."
    )
    st.info(analisis_ia)

    if st.button("🔄 Jugar una Nueva Partida"):
        st.session_state.clear()
        st.rerun()