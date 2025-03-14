Esta API permite gestionar equipos, jugadores y torneos en la plataforma "Más Que Amigos".

🔹 Tecnologías: Spring Boot, Java 8, JPA, H2 Database(Momentaneamente)
🔹 Formato de datos: JSON

#######################################################################
Versión	- Descripción	                                      -  Estado
v1.0	  - Primera versión con CRUD de equipos y jugadores.	- ✅ Activo

#######################################################################
URL'S :
  Equipo
    Base URL: 
      POST - http://localhost:8080/api/equipos
      
          Crea un nuevo equipo
          Ejemplo de Request (JSON)
          {
            "nombre": "Nacional",
            "directorTecnico": "Rueda",
            "imagenUrl": "https://example.com/real.png",
            "titulos": 10
          }
          
          Ejemplo de Response (JSON)
          {
             "id": null,
             "nombre": null,
             "directorTecnico": null,
             "imagenUrl": null,
             "titulos": 0,
             "idTorneo": 0,
             "status": "Success",
             "mensaje": "¡Gooool! El equipo Nacional se guardó en la base de datos."
          }

       PUT - http://localhost:8080/api/equipos
      
          Crea un nuevo equipo
          Ejemplo de Request (JSON)
          {
            "nombre": "CALI",
            "directorTecnico": "Rueda",
            "imagenUrl": "https://example.com/real.png",
            "titulos": 10
          }
          
          Ejemplo de Response (JSON)
          {
             "id": null,
             "nombre": null,
             "directorTecnico": null,
             "imagenUrl": null,
             "titulos": 0,
             "idTorneo": 0,
             "status": "Success",
             "mensaje": "¡Gooool! El equipo CALI se guardó en la base de datos."
          }
          
      Get - http://localhost:8080/api/equipos
      Lista los equipos almacenados en la BD
      
      Ejemplo de Response (JSON)
      [
        {
          "id": 1,
          "nombre": "Cali",
          "directorTecnico": "pecoso",
          "imagenUrl": "https://example.com/real.png",
          "titulos": 10,
          "idTorneo": 0,
          "indicadorRespuesta": "Success",
          "mensaje": "",
            "jugadores": [
              {
                "id": 1,
                "nombre": "Alex",
                "posicion": "DELANTERO",
                "edad": 26,
                "dorsal": 11,
                "indicadorRespuesta": null,
                "mensaje": null
              }
            ]
        }
      ]

     GET - http://localhost:8080/api/equipos/{nombre}
     Consulta un equipo por nombre.
  
     Ejemplo de Response (JSON)
      [
          {
            "id": 1,
            "nombre": "Cali",
            "directorTecnico": "pecoso",
            "imagenUrl": "https://example.com/real.png",
            "titulos": 10,
            "idTorneo": 0,
            "indicadorRespuesta": "Success",
            "mensaje": "",
              "jugadores": [
                {
                  "id": 1,
                  "nombre": "Alex",
                  "posicion": "DELANTERO",
                  "edad": 26,
                  "dorsal": 11,
                  "indicadorRespuesta": null,
                  "mensaje": null
                }
              ]
          }
      ]
  
     DELETE- http://localhost:8080/api/equipos/{id}
     Elimina un equipo por ID.
  
     Ejemplo de Response (JSON)
            {
               "id": null,
               "nombre": null,
               "directorTecnico": null,
               "imagenUrl": null,
               "titulos": 0,
               "idTorneo": 0,
               "status": "Success",
               "mensaje": "Fin del juego! El equipo Los Titanes ha sido eliminado correctamente."
            }

Jugadores
 Base URL: 
   POST - http://localhost:8080/api/v1/jugadores
    Crea un nuevo jugador
          Ejemplo de Request (JSON)
            {
              "nombre": "Alex",
              "posicion": "DELANTERO",
              "edad": 26,
              "dorsal": 11,
              "idEquipo": 1
            }
         Ejemplo de Response (JSON)
           {
             "id": null,
             "nombre": null,
             "posicion": null,
             "edad": 0,
             "dorsal": 0,
             "indicadorRespuesta": "Success",
             "mensaje": "Gooool! El jugador Alexse guardÃ³ en la base de datos."
          }
     
     GET - http://localhost:8080/api/v1/jugadores
     Consulta los jugadores.
  
     Ejemplo de Response (JSON)
     [{
       "id": 1,
       "nombre": "Alex",
       "posicion": "DELANTERO",
       "edad": 26,
       "dorsal": 11,
       "indicadorRespuesta": "Success",
       "mensaje": "",
       "idEquipo": 1,
       "equipo":    {
          "id": 1,
          "nombre": "Cali",
          "directorTecnico": "pecoso",
          "imagenUrl": "https://example.com/real.png",
          "titulos": 10,
          "idTorneo": 0,
          "indicadorRespuesta": null,
          "mensaje": null
       }
    }]

   GET - http://localhost:8080/api/jugadores/{dorsal}
   Consulta un jugador por dorsal.
  
     Ejemplo de Response (JSON)    
     [{
       "id": 1,
       "nombre": "Alex",
       "posicion": "DELANTERO",
       "edad": 26,
       "dorsal": 11,
       "indicadorRespuesta": "Success",
       "mensaje": "",
       "idEquipo": 1,
       "equipo":    {
          "id": 1,
          "nombre": "Cali",
          "directorTecnico": "pecoso",
          "imagenUrl": "https://example.com/real.png",
          "titulos": 10,
          "idTorneo": 0,
          "indicadorRespuesta": null,
          "mensaje": null
       }
    }]

   DELETE - http://localhost:8080/api/jugadores/{id}
   Elimina un jugador por id.

      Ejemplo de Response (JSON)
           {
             "id": null,
             "nombre": null,
             "posicion": null,
             "edad": 0,
             "dorsal": 0,
             "indicadorRespuesta": "Success",
             "mensaje": "Success, Fin del juego! El jugador Alex ha sido eliminado correctamente."
          }
    
    POST - http://localhost:8080/api/v1/jugadores
    Actualiza un jugador
          Ejemplo de Request (JSON)
            {
              "nombre": "Marino",
              "posicion": "DELANTERO",
              "edad": 26,
              "dorsal": 11,
              "idEquipo": 1
            }
         Ejemplo de Response (JSON)
           {
             "id": null,
             "nombre": null,
             "posicion": null,
             "edad": 0,
             "dorsal": 0,
             "indicadorRespuesta": "Success",
             "mensaje": "Gooool! El jugador Marino se guardÃ³ en la base de datos."
          }
     
