#Build
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Maven Wrapper dosyalarını kopyala
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Wrapper'ı çalıştırılabilir yap
RUN chmod +x mvnw

# Sadece bağımlılıkları indir
RUN ./mvnw dependency:go-offline

# Geri kalan kaynak kodunu kopyala
COPY src ./src

# Projeyi paketle (testleri atla)
RUN ./mvnw package -Dmaven.test.skip=true


#Run
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Sadece derlenmiş olan .jar dosyasını 'build' aşamasından kopyala
COPY --from=build /app/target/meetingroom-reservation-0.0.1-SNAPSHOT.jar app.jar

# Render'ın varsayılan portu 10000'dir.
EXPOSE 10000

# Uygulamayı başlat. Render'ın $PORT değişkenini kullan,
# bulamazsa 10000'i kullan. Bu, application.properties'deki 8080'i ezer.
CMD ["java", "-jar", "-Dserver.port=${PORT:-10000}", "app.jar"]