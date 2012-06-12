package org.dbpedia.spotlight.db.io

import io.Source
import scala.Predef._
import org.dbpedia.spotlight.model._
import java.io.{File, FileInputStream, InputStream}
import org.dbpedia.spotlight.model.Factory.OntologyType
import collection.immutable.HashMap
import scala.collection.JavaConverters._
import java.util.NoSuchElementException
import scala.collection.mutable.HashSet


/**
 * @author Joachim Daiber
 *
 */

/**
 * Represents a source of DBpediaResources
 */

object DBpediaResourceSource {

  def fromTSVInputStream(
    conceptList: InputStream,
    counts: InputStream,
    instanceTypes: InputStream
  ): java.util.Map[DBpediaResource, Int] = {

    var id = 0

    //The list of concepts may contain non-unique elements, hence convert it to a Set first to make sure
    //we do not count elements more than once.
    val resourceMap: Map[String, DBpediaResource] = (Source.fromInputStream(conceptList).getLines().toSet map {
      line: String => {
        val res = new DBpediaResource(line.trim)
        res.id = id
        id += 1
        Pair(res.uri, res)
      }
    }).toMap

    //Read counts:
    Source.fromInputStream(counts).getLines() foreach {
      line: String => {
        val Array(uri: String, count: String) = line.trim().split('\t')
        resourceMap(new DBpediaResource(uri).uri).setSupport(count.toInt)
      }
    }

    //Read types:
    val uriNotFound = HashSet[String]()
    Source.fromInputStream(instanceTypes).getLines() foreach {
      line: String => {
        val Array(id: String, typeURI: String) = line.trim().split('\t')

        try {
          if (! typeURI.startsWith(SchemaOrgType.SCHEMAORG_PREFIX))
            resourceMap(new DBpediaResource(id).uri).types ::= OntologyType.fromURI(typeURI)
        } catch {
          case e: NoSuchElementException =>
            //System.err.println("WARNING: DBpedia resource not in concept list %s (%s)".format(id, typeURI) )
            uriNotFound += id
        }
      }
    }
    System.err.println("WARNING: URI for %d type definitions not found!".format(uriNotFound.size) )

    resourceMap.iterator.map( f => Pair(f._2, f._2.support) ).toMap.asJava
  }


  def fromTSVFile(
    conceptList: File,
    counts: File,
    instanceTypes: File
  ): java.util.Map[DBpediaResource, Int] = fromTSVInputStream(
      new FileInputStream(conceptList),
      new FileInputStream(counts),
      new FileInputStream(instanceTypes)
    )

}