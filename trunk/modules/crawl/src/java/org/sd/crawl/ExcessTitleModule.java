/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.crawl;


import org.sd.text.DetailedUrl;

/**
 * A site info module that keeps track of excess titles.
 * <p>
 * @author Spence Koehler
 */
public class ExcessTitleModule extends BaseSuccessfulSiteInfoModule {
  
  public static final int MAX_TITLE_LEN = 60;


  public ExcessTitleModule() {
    super();
  }

  protected final void doAdd(CrawledPage crawledPage, int crawlDepth, DetailedUrl dUrl) {
    final String url = dUrl.getNormalizedUrl();

    final String title = crawledPage.getTitle();
    if (title != null && !"".equals(title) && title.length() > MAX_TITLE_LEN) {
      super.addString("", url);  // keep track of urls for pages w/excess title
    }
  }
}