/**
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.scalaleafs

class Configuration (assignments : ConfigVar.Assignment[_]*) {
  
  private val values : Map[ConfigVar[_], Any] = assignments.toMap
  
  // Get the configuration value for given configVar. 
  def apply[A](configVar : ConfigVar[A]) = 
    values.get(configVar).map(_.asInstanceOf[A]).getOrElse(configVar.defaultValue)
    
  def withDefaults(defaults : ConfigVar.Assignment[_]*) = {
    new Configuration(assignments ++ defaults.filterNot(a => values.contains(a._1)):_*)
  }
  
  def &(assignments : ConfigVar.Assignment[_]*) = {
    new Configuration(this.assignments ++ assignments:_*)
  }
}

abstract class ConfigVar[A](val defaultValue : A) 

object ConfigVar {
  
  // Type def to make sure each individual tuple has matching types for var and value.
  type Assignment[A] = Tuple2[ConfigVar[A], A]

  // Implicitly convert a configVar value to its value. 
  implicit def toValue[A](configVar : ConfigVar[A]) = R.configuration(configVar) 
}
