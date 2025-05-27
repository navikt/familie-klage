package no.nav.familie.klage.brevmottaker.dto

import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonUtenIdent
import no.nav.familie.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DtoTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class NyBrevmottakerDtoKtTest {
    @Nested
    inner class NyBrevmottakerPersonDtoTest {
        @Test
        fun `skal mappe NyBrevmottakerPersonMedIdentDto til domene`() {
            // Arrange
            val nyBrevmottakerPersonMedIdentDto = DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto()

            // Act
            val domene = (nyBrevmottakerPersonMedIdentDto as NyBrevmottakerPersonDto).tilDomene()

            // Assert
            assertThat(domene).isInstanceOfSatisfying(NyBrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(nyBrevmottakerPersonMedIdentDto.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonMedIdentDto.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonMedIdentDto.navn)
            }
        }

        @Test
        fun `skal mappe NyBrevmottakerPersonUtenIdentDto til domene`() {
            // Arrange
            val nyBrevmottakerPersonUtenIdentDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto()

            // Act
            val domene = (nyBrevmottakerPersonUtenIdentDto as NyBrevmottakerPersonDto).tilDomene()

            // Assert
            assertThat(domene).isInstanceOfSatisfying(NyBrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdentDto.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdentDto.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdentDto.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdentDto.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdentDto.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdentDto.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdentDto.landkode)
            }
        }
    }

    @Nested
    inner class NyBrevmottakerDtoTest {
        @Test
        fun `skal mappe til domene for NyBrevmottakerPersonUtenIdentDto`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                    mottakerRolle = MottakerRolle.FULLMAKT,
                    navn = "Navn Navnesen",
                    adresselinje1 = "Adresselinje 1",
                    adresselinje2 = "Adresselinje 2",
                    postnummer = "0010",
                    poststed = "Oslo",
                    landkode = "NO",
                )

            // Act
            val nyBrevmottaker = (nyBrevmottakerDto as NyBrevmottakerDto).tilDomene()

            // Assert
            assertThat(nyBrevmottaker).isInstanceOfSatisfying(NyBrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerDto.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerDto.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerDto.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerDto.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerDto.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerDto.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerDto.landkode)
            }
        }

        @Test
        fun `skal mappe til domene for NyBrevmottakerPersonMedIdentDto`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                    personIdent = "123",
                    mottakerRolle = MottakerRolle.FULLMAKT,
                    navn = "Navn Navnesen",
                )

            // Act
            val nyBrevmottaker = (nyBrevmottakerDto as NyBrevmottakerDto).tilDomene()

            // Assert
            assertThat(nyBrevmottaker).isInstanceOfSatisfying(NyBrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(nyBrevmottakerDto.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerDto.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerDto.navn)
            }
        }

        @Test
        fun `skal mappe til domene for NyBrevmottakerOrganisasjonDto`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerOrganisasjonDto(
                    organisasjonsnummer = "123",
                    organisasjonsnavn = "organisasjonsnavn",
                    navnHosOrganisasjon = "navnHosOrganisasjon",
                )

            // Act
            val nyBrevmottaker = (nyBrevmottakerDto as NyBrevmottakerDto).tilDomene()

            // Assert
            assertThat(nyBrevmottaker).isInstanceOfSatisfying(NyBrevmottakerOrganisasjon::class.java) {
                assertThat(it.organisasjonsnummer).isEqualTo(nyBrevmottakerDto.organisasjonsnummer)
                assertThat(it.organisasjonsnavn).isEqualTo(nyBrevmottakerDto.organisasjonsnavn)
                assertThat(it.navnHosOrganisasjon).isEqualTo(nyBrevmottakerDto.navnHosOrganisasjon)
            }
        }
    }

    @Nested
    inner class NyBrevmottakerOrganisasjonDtoTest {
        @Test
        fun `skal ha riktig type`() {
            // Arrange
            val dto = DtoTestUtil.lagNyBrevmottakerOrganisasjonDto()

            // Act
            val type = dto.type

            // Assert
            assertThat(type).isEqualTo(NyBrevmottakerDto.Type.ORGANISASJON)
        }
    }

    @Nested
    inner class NyBrevmottakerPersonMedIdentDtoTest {
        @Test
        fun `skal ha riktig type`() {
            // Arrange
            val dto = DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto()

            // Act
            val type = dto.type

            // Assert
            assertThat(type).isEqualTo(NyBrevmottakerDto.Type.PERSON_MED_IDENT)
        }

        @Test
        fun `skal kaste exception om MottakerRolle er BRUKER_MED_UTENLANDSK_ADRESSE`() {
            // Arrange
            val dto =
                DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                    mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    dto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Person med ident kan ikke være ${MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE}")
        }

        @Test
        fun `skal returnere false om person ident er ulik`() {
            // Arrange
            val nyBrevmottakerPersonMedIdentDto =
                DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            val brevmottakerPersonMedIdent =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    personIdent = "3",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            // Act
            val erLik = nyBrevmottakerPersonMedIdentDto.erLik(brevmottakerPersonMedIdent)

            // Assert
            assertThat(erLik).isFalse()
        }

        @Test
        fun `skal returnere false om navn er ulik`() {
            // Arrange
            val nyBrevmottakerPersonMedIdentDto =
                DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "foo",
                )

            val brevmottakerPersonMedIdent =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "bar",
                )

            // Act
            val erLik = nyBrevmottakerPersonMedIdentDto.erLik(brevmottakerPersonMedIdent)

            // Assert
            assertThat(erLik).isFalse()
        }

        @Test
        fun `skal returnere false om mottaker rolle er ulik`() {
            // Arrange
            val nyBrevmottakerPersonMedIdentDto =
                DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            val brevmottakerPersonMedIdent =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.VERGE,
                    navn = "navn",
                )

            // Act
            val erLik = nyBrevmottakerPersonMedIdentDto.erLik(brevmottakerPersonMedIdent)

            // Assert
            assertThat(erLik).isFalse()
        }

        @Test
        fun `skal returnere true om er like`() {
            // Arrange
            val nyBrevmottakerPersonMedIdentDto =
                DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            val brevmottakerPersonMedIdent =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            // Act
            val erLik = nyBrevmottakerPersonMedIdentDto.erLik(brevmottakerPersonMedIdent)

            // Assert
            assertThat(erLik).isTrue()
        }
    }

    @Nested
    inner class NyBrevmottakerPersonUtenIdentDtoTest {
        @Test
        fun `skal ha riktig type`() {
            // Arrange
            val dto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto()

            // Act
            val type = dto.type

            // Assert
            assertThat(type).isEqualTo(NyBrevmottakerDto.Type.PERSON_UTEN_IDENT)
        }

        @Test
        fun `skal kaste exception om mottakertype er bruker`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                    mottakerRolle = MottakerRolle.BRUKER,
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Det er ikke mulig å sette ${MottakerRolle.BRUKER} for saksbehandler.")
        }

        @Test
        fun `skal kaste exception om landkode er mindre enn 2 tegn`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(landkode = "N")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Ugyldig landkode: N.")
        }

        @Test
        fun `skal kaste exception om landkode er mer enn 2 tegn`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(landkode = "NOR")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Ugyldig landkode: NOR.")
        }

        @Test
        fun `skal kaste exception om navn er en tom string`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(navn = "")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Navn kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om navn er en string med kun mellomrom`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(navn = " ")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Navn kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om adresselinje1 er en tom string`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(adresselinje1 = "")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Adresselinje 1 kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om adresselinje1 er en string med kun mellomrom`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(adresselinje1 = " ")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Adresselinje 1 kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om postnummer er null og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(landkode = "NO", postnummer = null)

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Når landkode er NO (Norge) må postnummer være satt.")
        }

        @Test
        fun `skal kaste exception om poststed er null og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(landkode = "NO", poststed = null)

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Når landkode er NO (Norge) må poststed være satt.")
        }

        @Test
        fun `skal kaste exception om postnummer ikke inneholder kun tall og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(landkode = "NO", postnummer = "123T")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om postnummer er mindre enn 4 tegn og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(landkode = "NO", postnummer = "123")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om postnummer er mer enn 4 tegn og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(landkode = "NO", postnummer = "12345")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om MottakerRolle er bruker med utenlandsk adresse og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                    landkode = "NO",
                    mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Bruker med utenlandsk adresse kan ikke ha landkode NO.")
        }

        @Test
        fun `skal kaste exception om postnummer ikke er null og landkode ikke er NO`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                    landkode = "BR",
                    postnummer = "1234",
                    poststed = null,
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Ved utenlandsk landkode må postnummer settes i adresselinje 1.")
        }

        @Test
        fun `skal kaste exception om poststed ikke er null og landkode ikke er NO`() {
            // Arrange
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                    landkode = "BR",
                    postnummer = null,
                    poststed = "Harstad",
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    nyBrevmottakerDto.valider()
                }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Ved utenlandsk landkode må poststed settes i adresselinje 1.")
        }

        @Test
        fun `skal opprette ny brevmottaker med utenlandsk landekode`() {
            // Act
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                    mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                    navn = "Navn Navnesen",
                    adresselinje1 = "Adresseveien 1, Danmark, 0010",
                    adresselinje2 = null,
                    landkode = "DK",
                    postnummer = null,
                    poststed = null,
                )

            // Assert
            assertThat(nyBrevmottakerDto.mottakerRolle).isEqualTo(MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE)
            assertThat(nyBrevmottakerDto.navn).isEqualTo("Navn Navnesen")
            assertThat(nyBrevmottakerDto.adresselinje1).isEqualTo("Adresseveien 1, Danmark, 0010")
            assertThat(nyBrevmottakerDto.adresselinje2).isNull()
            assertThat(nyBrevmottakerDto.postnummer).isNull()
            assertThat(nyBrevmottakerDto.poststed).isNull()
            assertThat(nyBrevmottakerDto.landkode).isEqualTo("DK")
        }

        @Test
        fun `skal opprette ny brevmottaker med norsk landekode`() {
            // Act
            val nyBrevmottakerDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                    mottakerRolle = MottakerRolle.FULLMAKT,
                    navn = "Navn Navnesen",
                    adresselinje1 = "Adresseveien 1",
                    adresselinje2 = null,
                    landkode = "NO",
                    postnummer = "0010",
                    poststed = "Oslo",
                )

            // Assert
            assertThat(nyBrevmottakerDto.mottakerRolle).isEqualTo(MottakerRolle.FULLMAKT)
            assertThat(nyBrevmottakerDto.navn).isEqualTo("Navn Navnesen")
            assertThat(nyBrevmottakerDto.adresselinje1).isEqualTo("Adresseveien 1")
            assertThat(nyBrevmottakerDto.adresselinje2).isNull()
            assertThat(nyBrevmottakerDto.postnummer).isEqualTo("0010")
            assertThat(nyBrevmottakerDto.poststed).isEqualTo("Oslo")
            assertThat(nyBrevmottakerDto.landkode).isEqualTo("NO")
        }
    }

    @Nested
    inner class NyBrevmottakerDtoDeserializerTest {
        private val nyBrevmottakerDtoDeserializer: NyBrevmottakerDtoDeserializer = NyBrevmottakerDtoDeserializer()

        @Test
        fun `skal deserialisere NyBrevmottakerOrganisasjon`() {
            // Arrange
            val json =
                "{" +
                    "\"type\":\"ORGANISASJON\"," +
                    "\"organisasjonsnummer\":\"123\"," +
                    "\"organisasjonsnavn\":\"Orgnavn\"," +
                    "\"navnHosOrganisasjon\":\"OG\"" +
                    "}"

            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize = nyBrevmottakerDtoDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(NyBrevmottakerOrganisasjonDto::class.java) {
                assertThat(it.organisasjonsnummer).isEqualTo("123")
                assertThat(it.organisasjonsnavn).isEqualTo("Orgnavn")
                assertThat(it.navnHosOrganisasjon).isEqualTo("OG")
            }
        }

        @Test
        fun `skal deserialisere NyBrevmottakerPersonMedIdentDto`() {
            // Arrange
            val json =
                "{" +
                    "\"type\":\"PERSON_MED_IDENT\"," +
                    "\"personIdent\":\"01492350318\"," +
                    "\"mottakerRolle\":\"BRUKER\"," +
                    "\"navn\":\"Fornavn mellomnavn Etternavn\"" +
                    "}"

            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize = nyBrevmottakerDtoDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(NyBrevmottakerPersonMedIdentDto::class.java) {
                assertThat(it.personIdent).isEqualTo("01492350318")
                assertThat(it.mottakerRolle).isEqualTo(MottakerRolle.BRUKER)
                assertThat(it.navn).isEqualTo("Fornavn mellomnavn Etternavn")
            }
        }

        @Test
        fun `skal deserialisere norsk NyBrevmottakerPersonUtenIdentDto`() {
            // Arrange
            val json =
                "{" +
                    "\"type\":\"PERSON_UTEN_IDENT\"," +
                    "\"mottakerRolle\":\"FULLMAKT\"," +
                    "\"navn\":\"Fornavn mellomnavn Etternavn\"," +
                    "\"adresselinje1\":\"Adresse 1\"," +
                    "\"adresselinje2\":\"Adresse 2\"," +
                    "\"postnummer\":\"0010\"," +
                    "\"poststed\":\"Oslo\"," +
                    "\"landkode\":\"NO\"" +
                    "}"

            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize = nyBrevmottakerDtoDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(NyBrevmottakerPersonUtenIdentDto::class.java) {
                assertThat(it.mottakerRolle).isEqualTo(MottakerRolle.FULLMAKT)
                assertThat(it.navn).isEqualTo("Fornavn mellomnavn Etternavn")
                assertThat(it.adresselinje1).isEqualTo("Adresse 1")
                assertThat(it.adresselinje2).isEqualTo("Adresse 2")
                assertThat(it.postnummer).isEqualTo("0010")
                assertThat(it.poststed).isEqualTo("Oslo")
                assertThat(it.landkode).isEqualTo("NO")
            }
        }

        @Test
        fun `skal deserialisere utenlandsk NyBrevmottakerPersonUtenIdentDto`() {
            // Arrange
            val json =
                "{" +
                    "\"type\":\"PERSON_UTEN_IDENT\"," +
                    "\"mottakerRolle\":\"BRUKER_MED_UTENLANDSK_ADRESSE\"," +
                    "\"navn\":\"Fornavn mellomnavn Etternavn\"," +
                    "\"adresselinje1\":\"Adresse 1, Mars, 1337\"," +
                    "\"landkode\":\"DK\"" +
                    "}"

            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize = nyBrevmottakerDtoDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(NyBrevmottakerPersonUtenIdentDto::class.java) {
                assertThat(it.mottakerRolle).isEqualTo(MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE)
                assertThat(it.navn).isEqualTo("Fornavn mellomnavn Etternavn")
                assertThat(it.adresselinje1).isEqualTo("Adresse 1, Mars, 1337")
                assertThat(it.adresselinje2).isNull()
                assertThat(it.postnummer).isNull()
                assertThat(it.poststed).isNull()
                assertThat(it.landkode).isEqualTo("DK")
            }
        }
    }
}
