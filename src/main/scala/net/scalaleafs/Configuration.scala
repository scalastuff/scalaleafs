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

class Configuration (assignments : ConfigVal.Assignment[_]*) {
  
  private[scalaleafs] val values : Map[ConfigVal[_], _] = assignments.toMap
  
  def withDefaults(defaults : ConfigVal.Assignment[_]*) = {
    new Configuration(assignments ++ defaults.filterNot(a => values.contains(a._1)):_*)
  }
  
  def &(assignments : ConfigVal.Assignment[_]*) = {
    new Configuration(this.assignments ++ assignments:_*)
  }
}

abstract class ConfigVal[A](val defaultValue : A) {
  
  def apply(context : Context) : A =
    apply(context.site.configuration)
    
  def apply(configuration : Configuration) : A = 
    configuration.values.get(this).map(_.asInstanceOf[A]).getOrElse(defaultValue)
    
  def get(implicit configuration : Configuration) : A = 
    configuration.values.get(this).map(_.asInstanceOf[A]).getOrElse(defaultValue)
}

object ConfigVal {
  
  // Type def to make sure each individual tuple has matching types for configVal and value.
  type Assignment[A] = Tuple2[ConfigVal[A], A]
  
  // Implicitly convert a configVal value to its value. 
  implicit def getfromContext[A](configVar : ConfigVal[A])(implicit context : Context) = configVar.apply (context)
}
