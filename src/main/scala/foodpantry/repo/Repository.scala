package foodpantry.repo

import java.io.File
import java.io.PrintWriter
import scala.io.Source
import scala.util.Try

// ai-assisted: #3
// why: Implement a generic repository supporting parametric polymorphism (S1-9) and private file-path encapsulation (S1-10) to store data safely.
class DataRepository[T](
  // S1-10: Encapsulation - Private field with adjacent rationale comment
  // Encapsulation: The filePath parameter is kept private to ensure that outer components cannot manipulate the file path of this repository directly, protecting the database file from accidental redirection or leakage.
  private val filePath: String,
  private val serializer: RecordSerializer[T]
) {

  // S1-12: Exception handling - File reading wrapped in Try. Resource closed in finally.
  def loadAll(): Try[List[T]] = {
    Try {
      val fileObj = new File(filePath)
      if (!fileObj.exists()) {
        val parentDir = fileObj.getParentFile
        if (parentDir != null && !parentDir.exists()) {
          parentDir.mkdirs()
        }
        fileObj.createNewFile()
        List.empty[T]
      } else {
        val sourceObj = Source.fromFile(fileObj, "UTF-8")
        try {
          val lines = sourceObj.getLines().toList
          lines.flatMap { line =>
            if (line.trim.isEmpty) None
            else serializer.deserialize(line).toOption
          }
        } finally {
          sourceObj.close()
        }
      }
    }
  }

  // S1-12: Exception handling - File writing wrapped in Try. Resource closed in finally.
  def saveAll(records: List[T]): Try[Unit] = {
    Try {
      val fileObj = new File(filePath)
      val parentDir = fileObj.getParentFile
      if (parentDir != null && !parentDir.exists()) {
        parentDir.mkdirs()
      }
      val writerObj = new PrintWriter(fileObj, "UTF-8")
      try {
        records.foreach { record =>
          writerObj.println(serializer.serialize(record))
        }
      } finally {
        writerObj.close()
      }
    }
  }

  def add(record: T): Try[Unit] = {
    loadAll().flatMap { currentList =>
      saveAll(currentList :+ record)
    }
  }

  def update(matchesFilter: T => Boolean, replacement: T): Try[Unit] = {
    loadAll().flatMap { currentList =>
      val updatedList = currentList.map { item =>
        if (matchesFilter(item)) replacement else item
      }
      saveAll(updatedList)
    }
  }

  def delete(matchesFilter: T => Boolean): Try[Unit] = {
    loadAll().flatMap { currentList =>
      val updatedList = currentList.filterNot(matchesFilter)
      saveAll(updatedList)
    }
  }
}
