package no.nav.familie.ef.klage.testutil

import no.nav.familie.klage.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.klage.personopplysninger.pdl.DeltBosted
import no.nav.familie.klage.personopplysninger.pdl.Dødsfall
import no.nav.familie.klage.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.familie.klage.personopplysninger.pdl.ForelderBarnRelasjon
import no.nav.familie.klage.personopplysninger.pdl.Fullmakt
import no.nav.familie.klage.personopplysninger.pdl.Fødsel
import no.nav.familie.klage.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.klage.personopplysninger.pdl.Kjønn
import no.nav.familie.klage.personopplysninger.pdl.KjønnType
import no.nav.familie.klage.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.klage.personopplysninger.pdl.Metadata
import no.nav.familie.klage.personopplysninger.pdl.Navn
import no.nav.familie.klage.personopplysninger.pdl.Opphold
import no.nav.familie.klage.personopplysninger.pdl.Oppholdsadresse
import no.nav.familie.klage.personopplysninger.pdl.PdlBarn
import no.nav.familie.klage.personopplysninger.pdl.PdlSøker
import no.nav.familie.klage.personopplysninger.pdl.Sivilstand
import no.nav.familie.klage.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.klage.personopplysninger.pdl.Telefonnummer
import no.nav.familie.klage.personopplysninger.pdl.TilrettelagtKommunikasjon
import no.nav.familie.klage.personopplysninger.pdl.UkjentBosted
import no.nav.familie.klage.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.klage.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

object PdlTestdataHelper {

    val metadataGjeldende = Metadata(historisk = false)

    fun lagKjønn(kjønnType: KjønnType = KjønnType.KVINNE) = Kjønn(kjønnType)

    fun lagNavn(
        fornavn: String = "Fornavn",
        mellomnavn: String? = "mellomnavn",
        etternavn: String = "Etternavn",
        historisk: Boolean = false
    ): Navn {
        return Navn(
            fornavn,
            mellomnavn,
            etternavn,
            Metadata(historisk = historisk)
        )
    }

    fun pdlSøker(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        bostedsadresse: List<Bostedsadresse> = emptyList(),
        dødsfall: List<Dødsfall> = emptyList(),
        forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
        fødsel: List<Fødsel> = emptyList(),
        folkeregisterpersonstatus: List<Folkeregisterpersonstatus> = emptyList(),
        fullmakt: List<Fullmakt> = emptyList(),
        kjønn: Kjønn? = null,
        kontaktadresse: List<Kontaktadresse> = emptyList(),
        navn: List<Navn> = emptyList(),
        opphold: List<Opphold> = emptyList(),
        oppholdsadresse: List<Oppholdsadresse> = emptyList(),
        sivilstand: List<Sivilstand> = emptyList(),
        statsborgerskap: List<Statsborgerskap> = emptyList(),
        telefonnummer: List<Telefonnummer> = emptyList(),
        tilrettelagtKommunikasjon: List<TilrettelagtKommunikasjon> = emptyList(),
        innflyttingTilNorge: List<InnflyttingTilNorge> = emptyList(),
        utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList(),
        vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt> = emptyList()
    ) =
        PdlSøker(
            adressebeskyttelse,
            bostedsadresse,
            dødsfall,
            forelderBarnRelasjon,
            fødsel,
            folkeregisterpersonstatus,
            fullmakt,
            listOfNotNull(kjønn),
            kontaktadresse,
            navn,
            opphold,
            oppholdsadresse,
            sivilstand,
            statsborgerskap,
            telefonnummer,
            tilrettelagtKommunikasjon,
            innflyttingTilNorge,
            utflyttingFraNorge,
            vergemaalEllerFremtidsfullmakt
        )

    fun pdlBarn(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        bostedsadresse: List<Bostedsadresse> = emptyList(),
        deltBosted: List<DeltBosted> = emptyList(),
        dødsfall: List<Dødsfall> = emptyList(),
        forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
        fødsel: Fødsel? = null,
        navn: Navn = lagNavn()
    ) =
        PdlBarn(
            adressebeskyttelse,
            bostedsadresse,
            deltBosted,
            dødsfall,
            forelderBarnRelasjon,
            listOfNotNull(fødsel),
            listOfNotNull(navn)
        )

    fun fødsel(år: Int = 2018, måned: Int = 1, dag: Int = 1): Fødsel =
        fødsel(LocalDate.of(år, måned, dag))

    fun fødsel(fødselsdato: LocalDate) =
        Fødsel(
            fødselsår = fødselsdato.year,
            fødselsdato = fødselsdato,
            metadata = metadataGjeldende,
            fødested = null,
            fødekommune = null,
            fødeland = null
        )

    fun ukjentBostedsadresse(
        bostedskommune: String = "1234",
        historisk: Boolean = false
    ) =
        Bostedsadresse(
            angittFlyttedato = null,
            gyldigFraOgMed = null,
            gyldigTilOgMed = null,
            coAdressenavn = null,
            utenlandskAdresse = null,
            vegadresse = null,
            ukjentBosted = UkjentBosted(bostedskommune),
            matrikkeladresse = null,
            metadata = Metadata(historisk)
        )
}
