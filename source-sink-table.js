import React from 'react';
import clsx from "clsx";
import Link from '@docusaurus/Link';

export default function SourceSinkTable() {
    let plugins = require('./plugins.mdx');
    return ( <div>


<p><strong>Provider:</strong> <i className='plugin-link flink-cdc-color'><a target='_blank' href='https://ververica.github.io/flink-cdc-connectors'>FlinkCDC</a></i><i className='plugin-link chunjun-color'><a target='_blank' href='https://dtstack.github.io/chunjun'>Chunjun</a></i><i className='plugin-link tis-color'><a target='_blank' href='https://github.com/qlangtech/tis'>TIS</a></i><i className='plugin-link datax-color'><a target='_blank' href='https://github.com/alibaba/DataX'>DataX</a></i></p>

<table style={{width: '100%', display: 'table'}}  border='1'>
<thead><tr><th rowspan='2'>类型</th><th colspan='2'>批量(DataX)</th><th colspan='2'>实时</th></tr>
<tr><th width='20%'>读</th><th width='20%'>写</th><th width='20%'>Source</th><th width='20%'>Sink</th></tr>
</thead><tbody>
<tr>
<td class='endtype-name'>Doris</td><td></td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechpluginsincrflinkchunjundorissinkchunjundorissinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>RocketMQ</td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link tis-color'><Link to={plugins.metadata.permalink+'#comqlangtechasyncmessageclientconsumerrocketmqlistenerfactory'}>1</Link></i></td><td></td></tr>
<tr>
<td class='endtype-name'>StarRocks</td><td></td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechtispluginsincrflinkchunjunstarrockssinkchunjunstarrockssinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>MySQL</td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link flink-cdc-color'><Link to={plugins.metadata.permalink+'#comqlangtechpluginsincrflinkcdcmysqlflinkcdcmysqlsourcefactory'}>1</Link></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechtispluginsincrflinkconnectorsourcemysqlsourcefactory'}>2</Link></i></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechtispluginsincrflinkconnectorsinkmysqlsinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>RabbitMQ</td><td></td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechtispluginsincrflinkchunjunrabbitmqsinkchujunrabbitmqsinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>TiDB</td><td></td><td></td><td></td><td></td></tr>
<tr>
<td class='endtype-name'>Postgres</td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechpluginsincrflinkchunjunpostgresqlsourcechunjunpostgresqlsourcefactory'}>1</Link></i></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechpluginsincrflinkchunjunpostgresqlsinkchunjunpostgresqlsinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>MongoDB</td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link flink-cdc-color'><Link to={plugins.metadata.permalink+'#comqlangtechpluginsincrflinkcdcmongdbflinkcdcmongodbsourcefactory'}>1</Link></i></td><td></td></tr>
<tr>
<td class='endtype-name'>AliyunOSS</td><td></td><td></td><td></td><td></td></tr>
<tr>
<td class='endtype-name'>Oracle</td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechpluginsincrflinkchunjunoraclesourcechunjunoraclesourcefactory'}>1</Link></i></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechpluginsincrflinkchunjunoraclesinkchunjunoraclesinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>Clickhouse</td><td></td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechpluginsincrflinkchunjunclickhousesinkchunjunclickhousesinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>ElasticSearch</td><td></td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link tis-color'><Link to={plugins.metadata.permalink+'#comqlangtechtispluginsincrflinkconnectorelasticsearch7elasticsearchsinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>Cassandra</td><td></td><td></td><td></td><td></td></tr>
<tr>
<td class='endtype-name'>Hudi</td><td></td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link tis-color'><Link to={plugins.metadata.permalink+'#comqlangtechtispluginsincrflinkconnectorhudihudisinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>AliyunODPS</td><td></td><td></td><td></td><td></td></tr>
<tr>
<td class='endtype-name'>Kafka</td><td></td><td></td><td></td><td><i className={clsx('tis-check')}></i><i className='plugin-link chunjun-color'><Link to={plugins.metadata.permalink+'#comqlangtechtispluginsincrflinkchunjunkafkasinkchujunkafkasinkfactory'}>1</Link></i></td></tr>
<tr>
<td class='endtype-name'>SqlServer</td><td></td><td></td><td></td><td></td></tr>
<tr>
<td class='endtype-name'>FTP</td><td></td><td></td><td></td><td></td></tr>
</tbody>
</table>

   </div> );
}
