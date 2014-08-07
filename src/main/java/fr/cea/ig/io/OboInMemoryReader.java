package fr.cea.ig.io;

import fr.cea.ig.io.model.obo.Cardinality;
import fr.cea.ig.io.model.obo.Relation;
import fr.cea.ig.io.model.obo.Relations;
import fr.cea.ig.io.model.obo.Term;
import fr.cea.ig.io.model.obo.TermRelations;
import fr.cea.ig.io.model.obo.UCR;
import fr.cea.ig.io.model.obo.UER;
import fr.cea.ig.io.model.obo.ULS;
import fr.cea.ig.io.model.obo.UPA;
import fr.cea.ig.io.model.obo.UPC;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OboInMemoryReader extends Reader{
    private Map<String,Term> mMapTerms;


    private String extractQuotedString( final String line ){
        String result = null;
        int quoteStart= line.indexOf("\"");
        int quoteEnd  = -1;

        if( quoteStart >= 0){
            quoteEnd = line.substring( quoteStart + 1 ).indexOf("\"");
            if( quoteEnd >= 0 )
                quoteStart++;
            result = line.substring(quoteStart, quoteStart + quoteEnd );
        }

        return result;
    }


    private Cardinality parseCardinality( final String line ){
        final String    cardinalityToken    = "cardinality";
        final String    orderToken          = "order";
        final String    isPrimaryToken      = "is_primary";
        final String    isAlternateToken    = "is_alternate";
        final String    directionToken      = "direction";
        String          number              = "";
        String          order               = "";
        Boolean         isPrimary           = false;
        Boolean         isAlternate         = false;
        String          direction           = "";
        String          tmp                 = null;
        int             cardinatilityPos    = line.indexOf( cardinalityToken );
        int             orderPos            = line.indexOf( orderToken );
        int             isPrimaryPos        = line.indexOf( isPrimaryToken );
        int             isAlternatePos      = line.indexOf( isAlternateToken );
        int             directionPos        = line.indexOf( directionToken );

        if( cardinatilityPos >=0 )
            number = extractQuotedString( line.substring(cardinatilityPos) );
        if( orderPos >=0 )
            order = extractQuotedString( line.substring(orderPos) );
        if( directionPos >=0 )
            order = extractQuotedString( line.substring(directionPos) );
        if( isPrimaryPos >=0 ){
            tmp = extractQuotedString( line.substring(isPrimaryPos) );
            if( tmp != null )
                isPrimary = ( "False".equals(tmp) ) ? false : true;
        }
        if( isAlternatePos >=0 ){
            tmp = extractQuotedString( line.substring(isAlternatePos) );
            if( tmp != null )
                isAlternate = ( "False".equals(tmp) ) ? false : true;
        }

        return new Cardinality( number, order, direction, isPrimary, isAlternate );
    }

    private static Term termFactory(final String id, final String name, final String namespace, final String definition, final Set<Relation>   has_input_compound, final Set<Relation> has_output_compound, final Set<Relation> part_of, final Relation isA, final Relation superPathway ) throws ParseException{
        Term term = null;
        if( namespace.equals("reaction") )
            term = new UCR( id, name, definition, new Relations(has_input_compound, has_output_compound, part_of ) );
        else if( namespace.equals("enzymatic_reaction") )
            term = new UER( id, name, definition, new Relations(has_input_compound, has_output_compound, part_of ) );
        else if( namespace.equals("linear_sub_pathway") )
            term = new ULS( id, name, definition, new Relations(has_input_compound, has_output_compound, part_of ) );
        else if( namespace.equals("pathway") )
            term = new UPA( id, name, definition, new Relations(has_input_compound, has_output_compound, part_of ), isA, superPathway );
        else if( namespace.equals("compound") )
            term = new UPC( id, name, definition );
        else
            throw new ParseException("Unknown namespace: " + namespace, -1 );
        return term;
    }


    private void saveTerm(final String id, final String name, final String namespace, final String definition, final Set<Relation>   has_input_compound, final Set<Relation> has_output_compound, final Set<Relation> part_of, final Relation isA, final Relation superPathway ) throws ParseException{
        Term term = termFactory( id, name, namespace, definition, has_input_compound, has_output_compound,  part_of, isA, superPathway );
        boolean isTermWithRelation = true;
        if( term instanceof UPC )
            isTermWithRelation = false;

        if( isTermWithRelation ){

            Set<Relation> relations = ((TermRelations) term).getRelation("part_of");
            TermRelations parent    = null;

            for( Relation relation : relations ){

                parent =  (TermRelations) mMapTerms.get( relation.getIdLeft() );

                if( parent != null )
                    parent.add( (TermRelations)term );
                else{
                    parent = new TermRelations( relation.getIdLeft(),"", "" );
                    parent.add( (TermRelations)term );
                    mMapTerms.put(relation.getIdLeft(), parent);
                }
            }

            TermRelations termRelation = (TermRelations) mMapTerms.get( id );
            if( termRelation != null ){
                List<List<Term>> childs =  termRelation.getChilds();
                ((TermRelations) term).addAll( childs );
            }

            mMapTerms.put(id, term);
        }
        else
            mMapTerms.put(id, term);
    }


    /**
     * @param type
     * @param line
     * @return Relation
     */
    private Relation parseRelationShip( final String type, final String line ){
        Cardinality     cardinality     = null;
        int             index           = 0;
        String          name            = "";
        String[]        cursor          = { "idLeft", "idRight", "name" };
        String          idLeft          = "";
        String          idRight         = "";
        String[]        splittedLine    = line.split("!");

        for( String item : splittedLine ){ // unbound array if do not fit expected format
            if( "idLeft".equals(cursor[ index ]) ){
                int cardinalityPos = item.indexOf("{cardinality");
                if( cardinalityPos != -1 ){
                    cardinality = parseCardinality( item.substring(cardinalityPos) );
                    idLeft      = item.substring( 0, cardinalityPos ).trim();
                }
                else
                    idLeft = item.trim();
            }
            else if( "idRight".equals(cursor[ index ]) ){
                idRight = item.trim();
            }
            else if( "name".equals(cursor[ index ]) ){
                name = item.trim();
            }
            index++;
        }

        return new Relation( type, idLeft, cardinality, idRight, name );
    }


    /**
     *
     * @param type
     * @param line
     * @return Relation
     */
    private Relation parseRelation( final String type, final String line ){
        Cardinality     cardinality     = null;
        String[]        splittedLine    = line.split("!");
        String          idLeft          = splittedLine[0].trim();
        String          name            = splittedLine[1].trim();
        String          idRight         = "";

        return new Relation( type, idLeft, cardinality, idRight, name );
    }

    private void init() throws ParseException, IOException {

        mMapTerms   = new HashMap<String,Term>();
        String line = mBuffer.readLine();

        String          id                  = null;
        String          name                = null;
        String          namespace           = null;
        String          definition          = null;
        Set<Relation>   has_input_compound  = new HashSet<Relation>();
        Set<Relation>   has_output_compound = new HashSet<Relation>();
        Set<Relation>   part_of             = new HashSet<Relation>();
        Relation        isA                 = null;
        Relation        superPathway        = null;
        final String    tokenInput          = "has_input_compound";
        final String    tokenOutput         = "has_output_compound";
        final String    tokenPartOf         = "part_of";
        final String    tokenIsA            = "is_a";
        final String    tokenSuperPathway   = "uniprot_super_pathway";

        while( line != null ){
            if( line.startsWith("[Typedef]") ){
                line                = mBuffer.readLine();
                boolean isRunning   = true;
                while( isRunning ){
                    if( line == null )
                        isRunning   = false;
                    else if( line.equals("") )
                        isRunning   = false;
                    else
                        line = mBuffer.readLine();
                }
            }
            else if( line.startsWith("[Term]") ){
                if( id != null ){
                    saveTerm(id, name, namespace, definition, has_input_compound, has_output_compound, part_of, isA, superPathway);
                    id                  = null;
                    name                = null;
                    namespace           = null;
                    definition          = null;
                    has_input_compound  = new HashSet<Relation>();
                    has_output_compound = new HashSet<Relation>();
                    part_of             = new HashSet<Relation>();
                    isA                 = null;
                    superPathway        = null;
                }
            }
            else if( line.startsWith("id:") )
                id = line.substring( 4 );
            else if( line.startsWith("name:") )
                name = line.substring( 6 );
            else if( line.startsWith("namespace:") )
                namespace = line.substring( 11 );
            else if( line.startsWith("def:") )
                definition = line.substring( 5 );
            else if( line.startsWith("relationship:") ){
                if( line.contains(tokenInput) )
                    has_input_compound.add( parseRelationShip( tokenInput, line.substring( line.indexOf(tokenInput) + tokenInput.length() ).trim() ) );
                else if( line.contains(tokenOutput) )
                    has_output_compound.add( parseRelationShip( tokenOutput, line.substring( line.indexOf(tokenOutput) + tokenOutput.length() ).trim() ) );
                else if( line.contains( tokenPartOf ) )
                    part_of.add( parseRelationShip( tokenPartOf, line.substring( line.indexOf(tokenPartOf) + tokenPartOf.length() ).trim() ) );
            }
            else if( line.startsWith( "is_a:" ) )
                isA = parseRelation( tokenIsA, line.substring( line.indexOf(tokenIsA) + tokenIsA.length() + 1 ).trim() );
            else if( line.startsWith( "uniprot_super_pathway:" ) )
                superPathway = parseRelation( tokenSuperPathway, line.substring( line.indexOf(tokenSuperPathway) + tokenSuperPathway.length() + 1 ).trim() );
            line = mBuffer.readLine();
        }
        saveTerm(id, name, namespace, definition, has_input_compound, has_output_compound, part_of, isA, superPathway );
        mIsr.close();
        mBuffer.close();

    }

    public OboInMemoryReader(InputStream is, int number_page_size, String charset) throws ParseException, IOException {
        super(is, number_page_size, charset);
        init();
    }

    public OboInMemoryReader(InputStream is, int number_page_size) throws ParseException, IOException {
        super(is, number_page_size);
        init();
    }

    public OboInMemoryReader(InputStream is) throws ParseException, IOException {
        super(is);
        init();
    }

    public OboInMemoryReader(String path, int number_page_size, String charset) throws ParseException, IOException {
        super(path, number_page_size, charset);

        init();
    }

    public OboInMemoryReader(String path, int number_page_size) throws ParseException, IOException  {
        super(path, number_page_size);
        init();
    }

    public OboInMemoryReader(String path) throws ParseException, IOException  {
        super(path);
        init();
    }

    /**
     * @param id term id to find
     * @return Term Object found
     */
    public Term getTerm( final String id ){
        return mMapTerms.get( id );
    }
}
